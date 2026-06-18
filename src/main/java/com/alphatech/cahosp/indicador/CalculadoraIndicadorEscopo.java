package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.comum.GeradorPseudoaleatorio;
import com.alphatech.cahosp.estoque.LoteRepository;
import com.alphatech.cahosp.estoque.PosicaoEstoqueRepository;
import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import com.alphatech.cahosp.previsao.PrevisaoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Recalcula o <strong>valor atual</strong> dos indicadores do dashboard no escopo de um filtro
 * (unidade/insumo), a partir dos dados reais — mantendo {@code baseline}/{@code meta}/historico do
 * edital (constantes do projeto). RF-IND/RF-DASH.
 *
 * <p>O que reage ao filtro, e de onde vem o numero:
 * <ul>
 *   <li>{@code ind-ruptura} (Taxa de desabastecimento de essenciais): essenciais em nivel critico
 *       ÷ total de essenciais no escopo;</li>
 *   <li>{@code ind-vencimento} (Perdas por vencimento): lotes vencidos com saldo ÷ total de lotes
 *       com saldo no escopo (aproximacao de perda — o sistema ainda nao registra descarte);</li>
 *   <li>{@code ind-mape} (Assertividade/MAPE): MAPE medio das previsoes do escopo;</li>
 *   <li>{@code ind-emergencial} (Compras emergenciais): <strong>valor mockado deterministico</strong>
 *       por (unidade, insumo) — placeholder ate existir o dominio de compras (nao ha dado real).</li>
 * </ul>
 * Os demais indicadores nao reagem ao filtro (vazio = mantem o valor do edital). Sem filtro
 * (unidade e insumo nulos) tambem retorna vazio: o dashboard sem filtro fica identico a rede.
 */
@Component
public class CalculadoraIndicadorEscopo {

    static final String COD_RUPTURA = "ind-ruptura";
    static final String COD_VENCIMENTO = "ind-vencimento";
    static final String COD_MAPE = "ind-mape";
    static final String COD_EMERGENCIAL = "ind-emergencial";

    private static final BigDecimal CEM = new BigDecimal("100");

    private final PosicaoEstoqueRepository posicaoRepository;
    private final LoteRepository loteRepository;
    private final PrevisaoRepository previsaoRepository;

    public CalculadoraIndicadorEscopo(PosicaoEstoqueRepository posicaoRepository,
                                      LoteRepository loteRepository,
                                      PrevisaoRepository previsaoRepository) {
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.previsaoRepository = previsaoRepository;
    }

    /**
     * Valor atual de um indicador no escopo, com o lastro absoluto (numerador/denominador) quando
     * se aplica. {@code numeradorAbsoluto}/{@code denominadorAbsoluto} nulos = manter o do edital.
     */
    public record Ajuste(BigDecimal atual, BigDecimal numeradorAbsoluto, BigDecimal denominadorAbsoluto) {
    }

    /**
     * Recalcula o atual do indicador no escopo. {@link Optional#empty()} quando nao ha filtro, o
     * indicador nao reage, ou nao ha dado no escopo (nesses casos mantem-se o valor do edital).
     */
    public Optional<Ajuste> ajustar(IndicadorMeta indicador, UUID unidadeId, UUID insumoId) {
        if (unidadeId == null && insumoId == null) {
            return Optional.empty();
        }
        return switch (indicador.getCodigo()) {
            case COD_RUPTURA -> ruptura(unidadeId, insumoId);
            case COD_VENCIMENTO -> vencimento(unidadeId, insumoId);
            case COD_MAPE -> mape(unidadeId, insumoId);
            case COD_EMERGENCIAL -> emergencialMock(indicador, unidadeId, insumoId);
            default -> Optional.empty();
        };
    }

    /** Taxa de desabastecimento de essenciais = essenciais criticos ÷ total de essenciais (%). */
    private Optional<Ajuste> ruptura(UUID unidadeId, UUID insumoId) {
        long total = posicaoRepository.contarEssenciais(unidadeId, insumoId);
        if (total == 0) {
            // Sem essenciais no escopo (ex.: insumo filtrado nao e essencial) — taxa indefinida.
            return Optional.empty();
        }
        long criticos = posicaoRepository.contarEssenciaisCriticos(unidadeId, insumoId);
        return Optional.of(new Ajuste(percentual(criticos, total), bd(criticos), bd(total)));
    }

    /** Perdas por vencimento = lotes vencidos com saldo ÷ total de lotes com saldo (%). */
    private Optional<Ajuste> vencimento(UUID unidadeId, UUID insumoId) {
        long total = loteRepository.contarComSaldo(unidadeId, insumoId);
        if (total == 0) {
            return Optional.empty();
        }
        long vencidos = loteRepository.contarVencidosComSaldo(LocalDate.now(), unidadeId, insumoId);
        return Optional.of(new Ajuste(percentual(vencidos, total), bd(vencidos), bd(total)));
    }

    /** Assertividade da previsao: MAPE medio do escopo (sem lastro absoluto). */
    private Optional<Ajuste> mape(UUID unidadeId, UUID insumoId) {
        BigDecimal media = previsaoRepository.mediaMape(unidadeId, insumoId);
        if (media == null) {
            return Optional.empty();
        }
        return Optional.of(new Ajuste(media.setScale(2, RoundingMode.HALF_UP), null, null));
    }

    /**
     * Compras emergenciais — <strong>MOCK</strong> deterministico por (unidade, insumo): nao ha
     * dominio de compras no sistema, entao geramos um valor estavel (mesma semente => mesmo valor)
     * que <em>varia</em> com o filtro, em vez de ficar fixo. Substituir quando a feature existir.
     */
    private Optional<Ajuste> emergencialMock(IndicadorMeta indicador, UUID unidadeId, UUID insumoId) {
        GeradorPseudoaleatorio gerador = GeradorPseudoaleatorio.comSemente(
                "emergencial:" + idOuTraco(unidadeId) + ":" + idOuTraco(insumoId));
        // Fator em [0.35, 1.05] sobre o valor da rede — varia por escopo, sem extrapolar demais.
        double fator = 0.35 + gerador.proximo() * 0.70;
        BigDecimal atual = indicador.getAtual()
                .multiply(BigDecimal.valueOf(fator))
                .setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new Ajuste(atual, null, null));
    }

    private BigDecimal percentual(long numerador, long denominador) {
        return bd(numerador).multiply(CEM)
                .divide(bd(denominador), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal bd(long valor) {
        return BigDecimal.valueOf(valor);
    }

    private String idOuTraco(UUID id) {
        return id == null ? "-" : id.toString();
    }
}
