package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.LimiarAlerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.alerta.dto.GeracaoAlertasResponse;
import com.alphatech.cahosp.estoque.CalculadoraEstoque;
import com.alphatech.cahosp.estoque.LoteRepository;
import com.alphatech.cahosp.estoque.PosicaoEstoqueRepository;
import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Motor de geracao de alertas por regra (RF-ALE-01/02). Deriva alertas do estado atual do estoque,
 * da cobertura (consumo medio) e da validade dos lotes, usando os <strong>limiares configurados</strong>
 * ({@link LimiarAlerta}, RF-ALE-03) — alterar um limiar muda o proximo ciclo de geracao:
 *
 * <ul>
 *   <li><strong>Desabastecimento:</strong> posicao de medicamento <em>essencial</em> com saldo
 *       abaixo do percentual configurado do estoque minimo; severidade pela cobertura restante.</li>
 *   <li><strong>Vencimento:</strong> lote com saldo e validade dentro da janela configurada;
 *       severidade pela antecedencia.</li>
 * </ul>
 *
 * <p><strong>Idempotencia/regeneracao:</strong> os alertas ainda {@code ABERTO} sao removidos e
 * recalculados (sempre refletem a condicao corrente). Alertas ja em tratamento ou resolvidos sao
 * preservados — e o motor evita recriar um alerta cuja chave natural ({tipo, medicamento, unidade}
 * ou {tipo, lote}) ja exista, para nao duplicar um caso que alguem ja esta tratando.
 */
@Service
public class GeradorAlerta {

    private static final EnumSet<Perfil> DESTINATARIOS_DESABASTECIMENTO =
            EnumSet.of(Perfil.OPERADOR, Perfil.GESTOR);
    private static final EnumSet<Perfil> DESTINATARIOS_VENCIMENTO =
            EnumSet.of(Perfil.OPERADOR);

    private final PosicaoEstoqueRepository posicaoRepository;
    private final LoteRepository loteRepository;
    private final AlertaRepository alertaRepository;
    private final LimiarAlertaService limiarService;
    private final CalculadoraEstoque calculadoraEstoque;
    private final CalculadoraAlerta calculadoraAlerta;

    public GeradorAlerta(PosicaoEstoqueRepository posicaoRepository,
                         LoteRepository loteRepository,
                         AlertaRepository alertaRepository,
                         LimiarAlertaService limiarService,
                         CalculadoraEstoque calculadoraEstoque,
                         CalculadoraAlerta calculadoraAlerta) {
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.alertaRepository = alertaRepository;
        this.limiarService = limiarService;
        this.calculadoraEstoque = calculadoraEstoque;
        this.calculadoraAlerta = calculadoraAlerta;
    }

    /**
     * Regenera os alertas tomando {@code referencia} como "hoje" para os calculos de prazo.
     * Sem N+1: as posicoes/lotes vem com relacionamentos em fetch join, a deduplicacao usa as
     * chaves carregadas de uma vez (Set em memoria) e a persistencia e em lote ({@code saveAll}).
     */
    @Transactional
    public GeracaoAlertasResponse gerar(LocalDate referencia) {
        LimiarAlerta limiares = limiarService.configuracao();
        long abertosRenovados = alertaRepository.deleteByStatus(StatusAlerta.ABERTO);

        List<Alerta> novos = new ArrayList<>();
        long desabastecimento = limiares.isDesabastecimentoAtivo()
                ? gerarDesabastecimento(limiares, novos) : 0;
        long vencimento = limiares.isVencimentoAtivo()
                ? gerarVencimento(referencia, limiares, novos) : 0;
        alertaRepository.saveAll(novos);

        long totalAtivo = alertaRepository.count();
        String mensagem = String.format(
                "%d alerta(s) gerado(s): %d de desabastecimento e %d de vencimento.",
                desabastecimento + vencimento, desabastecimento, vencimento);
        return new GeracaoAlertasResponse(
                desabastecimento, vencimento, abertosRenovados, totalAtivo, mensagem);
    }

    /** RF-ALE-01: medicamento essencial com saldo abaixo do limiar vira alerta de desabastecimento. */
    private long gerarDesabastecimento(LimiarAlerta limiares, List<Alerta> acc) {
        // Chave natural {medicamento, unidade} ja existente — dedup em memoria (1 query, sem N+1).
        Set<String> chaves = new HashSet<>();
        for (Object[] chave : alertaRepository.chavesPorMedicamentoEUnidade(TipoAlerta.DESABASTECIMENTO)) {
            chaves.add(chave[0] + ":" + chave[1]);
        }
        long gerados = 0;
        for (PosicaoEstoque pos : posicaoRepository.findAllComRelacionamentos()) {
            Medicamento med = pos.getMedicamento();
            boolean abaixoDoMinimo = calculadoraAlerta.abaixoDoEstoqueMinimo(
                    pos.getQuantidade(), pos.getNivelCritico(), limiares.getPercentualEstoqueMinimo());
            if (!abaixoDoMinimo || !med.isEssencial()) {
                continue;
            }
            // add() devolve false se a chave ja existe (no banco ou nesta mesma rodada) — evita duplicar.
            if (!chaves.add(med.getId() + ":" + pos.getUnidade().getId())) {
                continue;
            }
            long dias = calculadoraAlerta.coberturaDias(pos.getQuantidade(), pos.getConsumoMedioDiario());
            Severidade severidade = calculadoraAlerta.severidadePorCobertura(dias, limiares);
            String mensagem = String.format(
                    "Cobertura de %d dia(s) — abaixo do estoque mínimo (%d %s).",
                    dias, pos.getNivelCritico(), med.getUnidadeMedida());
            acc.add(new Alerta(TipoAlerta.DESABASTECIMENTO, severidade, med,
                    pos.getUnidade(), null, mensagem, DESTINATARIOS_DESABASTECIMENTO, (int) dias));
            gerados++;
        }
        return gerados;
    }

    /** RF-ALE-02: lote com saldo e validade na janela configurada vira alerta de vencimento. */
    private long gerarVencimento(LocalDate referencia, LimiarAlerta limiares, List<Alerta> acc) {
        Set<UUID> lotesComAlerta = new HashSet<>(alertaRepository.lotesComAlerta(TipoAlerta.VENCIMENTO));
        LocalDate validadeAte = referencia.plusDays(limiares.getAntecedenciaVencimentoDias());
        long gerados = 0;
        for (Lote lote : loteRepository.findVencendoComRelacionamentos(0, validadeAte)) {
            if (!lotesComAlerta.add(lote.getId())) {
                continue;
            }
            long dias = calculadoraEstoque.diasParaVencer(lote.getValidade(), referencia);
            Severidade severidade = calculadoraAlerta.severidadePorVencimento(dias, limiares);
            Medicamento med = lote.getMedicamento();
            String mensagem = String.format(
                    "Lote %s (%d %s) vence em %d dia(s).",
                    lote.getNumeroLote(), lote.getQuantidade(), med.getUnidadeMedida(), dias);
            acc.add(new Alerta(TipoAlerta.VENCIMENTO, severidade, med,
                    lote.getUnidade(), lote, mensagem, DESTINATARIOS_VENCIMENTO, (int) dias));
            gerados++;
        }
        return gerados;
    }
}
