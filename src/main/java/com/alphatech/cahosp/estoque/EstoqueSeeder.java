package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.GeradorPseudoaleatorio;
import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.estoque.dominio.Movimentacao;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import com.alphatech.cahosp.insumo.InsumoRepository;
import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Popula estoque/lotes/movimentacoes no startup (idempotente), portando o gerador determinístico
 * do front (mulberry32 + FNV-1a) — distribuicao realista: ~18% em nivel critico, ~14% em atencao,
 * e lotes proximos do vencimento. RF-EST.
 *
 * <p>Roda apos os seeders de catalogo ({@code @Order(30)}). A {@code quantidade} da posicao casa
 * com a soma dos saldos dos lotes (projecao do livro-razao).
 */
@Component
@Order(30)
public class EstoqueSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EstoqueSeeder.class);

    private static final LocalDate BASE_VALIDADE = LocalDate.of(2026, 6, 1);
    private static final String[] FABRICANTES = {
            "Eurofarma", "Hipolabor", "Blau", "Cristália", "Halex Istar", "Teuto", "União Química"};
    private static final String[] RESPONSAVEIS = {
            "A. Sousa", "M. Lima", "R. Costa", "J. Pereira", "C. Mendes"};

    private final InsumoRepository insumoRepository;
    private final UnidadeRepository unidadeRepository;
    private final PosicaoEstoqueRepository posicaoRepository;
    private final LoteRepository loteRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public EstoqueSeeder(InsumoRepository insumoRepository,
                         UnidadeRepository unidadeRepository,
                         PosicaoEstoqueRepository posicaoRepository,
                         LoteRepository loteRepository,
                         MovimentacaoRepository movimentacaoRepository) {
        this.insumoRepository = insumoRepository;
        this.unidadeRepository = unidadeRepository;
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Override
    public void run(String... args) {
        if (posicaoRepository.count() > 0) {
            log.info("Estoque ja semeado (posicoes: {}). Nada a fazer.", posicaoRepository.count());
            return;
        }
        List<Insumo> insumos = insumoRepository.findAll();
        // Unidades atendidas: exclui o hub logistico (CAHOSP nao consome diretamente).
        List<Unidade> unidades = unidadeRepository.findAll().stream()
                .filter(u -> !u.isHub())
                .toList();

        for (Insumo med : insumos) {
            for (Unidade uni : unidades) {
                GeradorPseudoaleatorio r =
                        GeradorPseudoaleatorio.comSemente("est" + med.getCodigo() + uni.getSigla());

                int consumoDiario = Math.max(1,
                        arredondar((baseDemanda(med, uni) / 30.0) * (0.9 + r.proximo() * 0.2)));
                int lead = arredondar(7 + r.proximo() * 22);
                int nivelCritico = arredondar(consumoDiario * (lead + 5.0));
                int estoqueMaximo = arredondar(consumoDiario * 60.0);

                double fator = r.proximo();
                int quantidade = fator < 0.18 ? arredondar(nivelCritico * (0.2 + r.proximo() * 0.5))
                        : fator < 0.32 ? arredondar(nivelCritico * (1 + r.proximo() * 0.2))
                        : arredondar(nivelCritico * (1.4 + r.proximo() * 1.4));

                posicaoRepository.save(new PosicaoEstoque(med, uni, quantidade,
                        nivelCritico, estoqueMaximo, consumoDiario, lead));
                semearLotes(med, uni, quantidade, r);
            }
        }
        log.info("Estoque semeado: {} posicoes, {} lotes, {} movimentacoes.",
                posicaoRepository.count(), loteRepository.count(), movimentacaoRepository.count());
    }

    /** Cria os lotes (somando a quantidade da posicao) e seu historico de movimentacoes. */
    private void semearLotes(Insumo med, Unidade uni, int quantidade, GeradorPseudoaleatorio r) {
        int nLotes = 1 + (int) Math.floor(r.proximo() * 2); // 1 ou 2
        int restante = quantidade;
        List<Lote> lotesSalvos = new ArrayList<>();
        for (int k = 0; k < nLotes; k++) {
            int qLote = (k == nLotes - 1) ? restante : arredondar(restante * (0.4 + r.proximo() * 0.3));
            restante -= qLote;

            double venc = r.proximo();
            int mesesValidade = venc < 0.2 ? 1 : venc < 0.35 ? 2 : 4 + (int) Math.floor(r.proximo() * 18);
            LocalDate validade = BASE_VALIDADE.plusMonths(mesesValidade)
                    .plusDays((int) Math.floor(r.proximo() * 27));
            String numeroLote = String.format("%s%s%04d",
                    med.getCodigo().replace("INS-", ""), uni.getSigla(),
                    1000 + (int) Math.floor(r.proximo() * 8999));
            String fabricante = FABRICANTES[(int) Math.floor(r.proximo() * FABRICANTES.length)];

            lotesSalvos.add(loteRepository.save(
                    new Lote(med, uni, numeroLote, validade, Math.max(0, qLote), fabricante)));
        }
        semearMovimentacoes(lotesSalvos, r);
    }

    private void semearMovimentacoes(List<Lote> lotes, GeradorPseudoaleatorio r) {
        List<Movimentacao> movs = new ArrayList<>();
        for (Lote lote : lotes) {
            int nMov = 2 + (int) Math.floor(r.proximo() * 3); // 2 a 4
            for (int j = 0; j < nMov; j++) {
                TipoMovimentacao tipo = j == 0 ? TipoMovimentacao.ENTRADA
                        : r.proximo() > 0.8 ? TipoMovimentacao.TRANSFERENCIA : TipoMovimentacao.SAIDA;
                int qMov = Math.max(1, arredondar(lote.getQuantidade() * (0.1 + r.proximo() * 0.4)));
                LocalDateTime quando = LocalDateTime.of(
                        2026, 3 + (int) Math.floor(r.proximo() * 3), 1 + (int) Math.floor(r.proximo() * 27),
                        8 + (int) Math.floor(r.proximo() * 9), (int) Math.floor(r.proximo() * 59));
                movs.add(new Movimentacao(lote, tipo, qMov,
                        quando.toInstant(ZoneOffset.UTC),
                        RESPONSAVEIS[(int) Math.floor(r.proximo() * RESPONSAVEIS.length)],
                        String.format("NF-%05d", 10000 + (int) Math.floor(r.proximo() * 89999))));
            }
        }
        movimentacaoRepository.saveAll(movs);
    }

    /** Demanda mensal base por porte/categoria/criticidade (espelha o front). */
    private int baseDemanda(Insumo med, Unidade uni) {
        double porte = uni.getPorte() == Porte.GRANDE ? 1.0
                : uni.getPorte() == Porte.MEDIO ? 0.55 : 0.3;
        double base = med.getCategoria() == CategoriaInsumo.INSUMOS_MEDICOS ? 4200
                : med.getCategoria() == CategoriaInsumo.SOROS_E_VACINAS ? 1800
                : med.getCriticidade() == Criticidade.ALTA ? 900 : 1400;
        return arredondar(base * porte);
    }

    private int arredondar(double v) {
        return (int) Math.round(v);
    }
}
