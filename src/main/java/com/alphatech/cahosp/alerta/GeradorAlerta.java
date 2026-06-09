package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.alerta.dto.GeracaoAlertasResponse;
import com.alphatech.cahosp.estoque.CalculadoraEstoque;
import com.alphatech.cahosp.estoque.LoteRepository;
import com.alphatech.cahosp.estoque.PosicaoEstoqueRepository;
import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/**
 * Motor de geracao de alertas por regra (RF-ALE-01/02). Deriva alertas do estado atual do estoque,
 * da cobertura (consumo medio) e da validade dos lotes — espelhando o algoritmo do front
 * (src/data/index.ts):
 *
 * <ul>
 *   <li><strong>Desabastecimento:</strong> para cada posicao de medicamento <em>essencial</em> em
 *       nivel critico, gera um alerta com severidade pela cobertura restante.</li>
 *   <li><strong>Vencimento:</strong> para cada lote com saldo e validade dentro da janela
 *       ({@value CalculadoraAlerta#JANELA_VENCIMENTO_DIAS} dias), gera um alerta com severidade
 *       pela antecedencia.</li>
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
    private final CalculadoraEstoque calculadoraEstoque;
    private final CalculadoraAlerta calculadoraAlerta;

    public GeradorAlerta(PosicaoEstoqueRepository posicaoRepository,
                         LoteRepository loteRepository,
                         AlertaRepository alertaRepository,
                         CalculadoraEstoque calculadoraEstoque,
                         CalculadoraAlerta calculadoraAlerta) {
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.alertaRepository = alertaRepository;
        this.calculadoraEstoque = calculadoraEstoque;
        this.calculadoraAlerta = calculadoraAlerta;
    }

    /** Regenera os alertas tomando {@code referencia} como "hoje" para os calculos de prazo. */
    @Transactional
    public GeracaoAlertasResponse gerar(LocalDate referencia) {
        long abertosRenovados = alertaRepository.deleteByStatus(StatusAlerta.ABERTO);

        long desabastecimento = gerarDesabastecimento();
        long vencimento = gerarVencimento(referencia);

        long totalAtivo = alertaRepository.count();
        String mensagem = String.format(
                "%d alerta(s) gerado(s): %d de desabastecimento e %d de vencimento.",
                desabastecimento + vencimento, desabastecimento, vencimento);
        return new GeracaoAlertasResponse(
                desabastecimento, vencimento, abertosRenovados, totalAtivo, mensagem);
    }

    /** RF-ALE-01: medicamento essencial em nivel critico vira alerta de desabastecimento. */
    private long gerarDesabastecimento() {
        long gerados = 0;
        for (PosicaoEstoque pos : posicaoRepository.findAll()) {
            Medicamento med = pos.getMedicamento();
            boolean critico = calculadoraEstoque.status(pos.getQuantidade(), pos.getNivelCritico())
                    == StatusEstoque.CRITICO;
            if (!critico || !med.isEssencial()) {
                continue;
            }
            if (alertaRepository.existsByTipoAndMedicamentoIdAndUnidadeIdAndLoteIsNull(
                    TipoAlerta.DESABASTECIMENTO, med.getId(), pos.getUnidade().getId())) {
                continue;
            }
            long dias = calculadoraAlerta.coberturaDias(pos.getQuantidade(), pos.getConsumoMedioDiario());
            Severidade severidade = calculadoraAlerta.severidadePorCobertura(dias);
            String mensagem = String.format(
                    "Cobertura de %d dia(s) — abaixo do estoque mínimo (%d %s).",
                    dias, pos.getNivelCritico(), med.getUnidadeMedida());
            alertaRepository.save(new Alerta(TipoAlerta.DESABASTECIMENTO, severidade, med,
                    pos.getUnidade(), null, mensagem, DESTINATARIOS_DESABASTECIMENTO, (int) dias));
            gerados++;
        }
        return gerados;
    }

    /** RF-ALE-02: lote com saldo e validade na janela vira alerta de vencimento. */
    private long gerarVencimento(LocalDate referencia) {
        LocalDate validadeAte = referencia.plusDays(CalculadoraAlerta.JANELA_VENCIMENTO_DIAS);
        List<Lote> lotes = loteRepository
                .findByQuantidadeGreaterThanAndValidadeLessThanEqualOrderByValidadeAsc(0, validadeAte);
        long gerados = 0;
        for (Lote lote : lotes) {
            if (alertaRepository.existsByTipoAndLoteId(TipoAlerta.VENCIMENTO, lote.getId())) {
                continue;
            }
            long dias = calculadoraEstoque.diasParaVencer(lote.getValidade(), referencia);
            Severidade severidade = calculadoraAlerta.severidadePorVencimento(dias);
            Medicamento med = lote.getMedicamento();
            String mensagem = String.format(
                    "Lote %s (%d %s) vence em %d dia(s).",
                    lote.getNumeroLote(), lote.getQuantidade(), med.getUnidadeMedida(), dias);
            alertaRepository.save(new Alerta(TipoAlerta.VENCIMENTO, severidade, med,
                    lote.getUnidade(), lote, mensagem, DESTINATARIOS_VENCIMENTO, (int) dias));
            gerados++;
        }
        return gerados;
    }
}
