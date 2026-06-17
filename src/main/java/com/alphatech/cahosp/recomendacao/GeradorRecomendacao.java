package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.estoque.CalculadoraEstoque;
import com.alphatech.cahosp.estoque.PosicaoEstoqueRepository;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import com.alphatech.cahosp.recomendacao.dto.GeracaoRecomendacoesResponse;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Motor de geracao de recomendacoes por regra (RF-REC-01/03). Deriva recomendacoes do estado do
 * estoque — espelhando o algoritmo do front (src/data/index.ts):
 *
 * <ul>
 *   <li><strong>Redistribuicao:</strong> para cada medicamento <em>essencial</em> com uma posicao
 *       em nivel critico e outra (em unidade diferente) com excedente, transfere para equilibrar.</li>
 *   <li><strong>Reposicao:</strong> para cada posicao fora do nivel ideal (critico/atencao),
 *       dimensiona a compra ate o estoque maximo.</li>
 * </ul>
 *
 * <p><strong>Idempotencia/regeneracao:</strong> as recomendacoes ainda {@code PENDENTE} sao
 * removidas e recalculadas; as ja {@code APROVADA}/{@code EXECUTADA} sao preservadas e nao
 * duplicadas (chave natural {tipo, medicamento, unidade destino}). Tudo nasce {@code PENDENTE}.
 */
@Service
public class GeradorRecomendacao {

    private final PosicaoEstoqueRepository posicaoRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final CalculadoraEstoque calculadoraEstoque;
    private final CalculadoraRecomendacao calculadoraRecomendacao;

    public GeradorRecomendacao(PosicaoEstoqueRepository posicaoRepository,
                               RecomendacaoRepository recomendacaoRepository,
                               CalculadoraEstoque calculadoraEstoque,
                               CalculadoraRecomendacao calculadoraRecomendacao) {
        this.posicaoRepository = posicaoRepository;
        this.recomendacaoRepository = recomendacaoRepository;
        this.calculadoraEstoque = calculadoraEstoque;
        this.calculadoraRecomendacao = calculadoraRecomendacao;
    }

    @Transactional
    public GeracaoRecomendacoesResponse gerar() {
        long renovadas = recomendacaoRepository.deleteByStatus(StatusRecomendacao.PENDENTE);

        // Posicoes com relacionamentos (sem N+1) e chaves existentes carregadas de uma vez (dedup em memoria).
        List<PosicaoEstoque> posicoes = posicaoRepository.findAllComRelacionamentos();
        Set<String> existentes = new HashSet<>();
        for (Object[] c : recomendacaoRepository.chavesExistentes()) {
            existentes.add(chave(c[0], c[1], c[2]));
        }
        // Contador deterministico (espelha recSeq do front): dirige origem do motor e economia.
        int[] seq = {1};
        List<Recomendacao> novos = new ArrayList<>();

        long redistribuicao = gerarRedistribuicao(posicoes, seq, existentes, novos);
        long reposicao = gerarReposicao(posicoes, seq, existentes, novos);
        recomendacaoRepository.saveAll(novos);

        long totalAtivo = recomendacaoRepository.count();
        String mensagem = String.format(
                "%d recomendacao(oes) gerada(s): %d de reposicao e %d de redistribuicao.",
                reposicao + redistribuicao, reposicao, redistribuicao);
        return new GeracaoRecomendacoesResponse(
                reposicao, redistribuicao, renovadas, totalAtivo, mensagem);
    }

    /** Chave natural de deduplicacao de uma recomendacao: {tipo, medicamento, unidade destino}. */
    private static String chave(Object tipo, Object medicamentoId, Object unidadeDestinoId) {
        return tipo + ":" + medicamentoId + ":" + unidadeDestinoId;
    }

    /** RF-REC-01: equilibra um medicamento essencial entre uma unidade em risco e outra com excedente. */
    private long gerarRedistribuicao(List<PosicaoEstoque> posicoes, int[] seq,
                                     Set<String> existentes, List<Recomendacao> acc) {
        Map<UUID, List<PosicaoEstoque>> porMedicamento = posicoes.stream()
                .collect(Collectors.groupingBy(p -> p.getMedicamento().getId()));

        List<Medicamento> essenciais = porMedicamento.values().stream()
                .map(lista -> lista.get(0).getMedicamento())
                .filter(Medicamento::isEssencial)
                .sorted(Comparator.comparing(Medicamento::getCodigo))
                .toList();

        long gerados = 0;
        for (Medicamento med : essenciais) {
            List<PosicaoEstoque> posicoesMed = porMedicamento.get(med.getId());
            PosicaoEstoque destino = posicoesMed.stream()
                    .filter(p -> status(p) == StatusEstoque.CRITICO)
                    .min(Comparator.comparingInt(PosicaoEstoque::getQuantidade))
                    .orElse(null);
            PosicaoEstoque origem = posicoesMed.stream()
                    .filter(p -> calculadoraRecomendacao.ehExcedente(p.getQuantidade(), p.getNivelCritico()))
                    .max(Comparator.comparingInt(PosicaoEstoque::getQuantidade))
                    .orElse(null);
            if (destino == null || origem == null
                    || origem.getUnidade().getId().equals(destino.getUnidade().getId())) {
                continue;
            }
            if (!existentes.add(chave(TipoRecomendacao.REDISTRIBUICAO, med.getId(), destino.getUnidade().getId()))) {
                continue;
            }
            int qtd = calculadoraRecomendacao.quantidadeRedistribuicao(
                    destino.getNivelCritico(), destino.getQuantidade());
            Unidade unidadeDestino = destino.getUnidade();
            Unidade unidadeOrigem = origem.getUnidade();
            String justificativa = String.format(
                    "%s abaixo do estoque mínimo; %s com excedente. Transferência evita compra emergencial.",
                    unidadeDestino.getSigla(), unidadeOrigem.getSigla());
            OrigemMotor motor = (seq[0] % 3 == 0)
                    ? OrigemMotor.APRENDIZADO_MAQUINA : OrigemMotor.REGRAS;
            BigDecimal economia = economia(qtd, 12 + (seq[0] % 9));

            acc.add(new Recomendacao(TipoRecomendacao.REDISTRIBUICAO, med,
                    unidadeDestino, unidadeOrigem, qtd, justificativa, motor,
                    Prioridade.ESSENCIAL, economia));
            seq[0]++;
            gerados++;
        }
        return gerados;
    }

    /** RF-REC-01: repoe ate o estoque maximo as posicoes fora do nivel ideal. */
    private long gerarReposicao(List<PosicaoEstoque> posicoes, int[] seq,
                                Set<String> existentes, List<Recomendacao> acc) {
        List<PosicaoEstoque> ordenadas = posicoes.stream()
                .sorted(Comparator.comparing((PosicaoEstoque p) -> p.getMedicamento().getCodigo())
                        .thenComparing(p -> p.getUnidade().getSigla()))
                .toList();

        long gerados = 0;
        for (PosicaoEstoque pos : ordenadas) {
            if (status(pos) == StatusEstoque.OK) {
                continue;
            }
            Medicamento med = pos.getMedicamento();
            if (!existentes.add(chave(TipoRecomendacao.REPOSICAO, med.getId(), pos.getUnidade().getId()))) {
                continue;
            }
            int qtd = calculadoraRecomendacao.quantidadeReposicao(
                    pos.getEstoqueMaximo(), pos.getQuantidade());
            String justificativa = String.format(
                    "Previsão de demanda indica reposição de %d %s para cobrir o horizonte de 30 dias "
                            + "e restaurar o nível de segurança.",
                    qtd, med.getUnidadeMedida());
            Prioridade prioridade = calculadoraRecomendacao.prioridadePorCriticidade(med.getCriticidade());
            BigDecimal economia = economia(qtd, 3 + (seq[0] % 5));

            acc.add(new Recomendacao(TipoRecomendacao.REPOSICAO, med,
                    pos.getUnidade(), null, qtd, justificativa, OrigemMotor.REGRAS,
                    prioridade, economia));
            seq[0]++;
            gerados++;
        }
        return gerados;
    }

    private StatusEstoque status(PosicaoEstoque p) {
        return calculadoraEstoque.status(p.getQuantidade(), p.getNivelCritico());
    }

    /** Economia estimada (R$) = quantidade x fator unitario evitado de compra emergencial. */
    private BigDecimal economia(int quantidade, int fatorUnitario) {
        return BigDecimal.valueOf((long) quantidade * fatorUnitario).setScale(2, RoundingMode.HALF_UP);
    }
}
