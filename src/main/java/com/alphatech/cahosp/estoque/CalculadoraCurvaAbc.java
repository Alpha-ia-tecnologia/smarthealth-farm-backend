package com.alphatech.cahosp.estoque;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Classificacao da Curva ABC (analise de Pareto) por valor de consumo — regra pura, sem banco,
 * facil de testar (RF-EST). Ordena os itens pelo valor (desc), acumula a participacao e classifica:
 * <ul>
 *   <li><b>A</b> — itens que respondem pelos primeiros ~80% do valor (os "vitais");</li>
 *   <li><b>B</b> — a faixa seguinte ate ~95%;</li>
 *   <li><b>C</b> — a cauda restante.</li>
 * </ul>
 * A classe usa o acumulado <em>antes</em> do item, de modo que o item que cruza a faixa de 80%
 * ainda entra em A (convencao usual da Curva ABC).
 */
@Component
public class CalculadoraCurvaAbc {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal LIMITE_A = new BigDecimal("80");
    private static final BigDecimal LIMITE_B = new BigDecimal("95");

    public enum Classe { A, B, C }

    /** Entrada do calculo: um insumo e o seu valor de consumo (consumo medio x custo unitario). */
    public record Entrada(UUID insumoId, BigDecimal valorConsumo) {}

    /** Item classificado, com participacao e acumulado em pontos percentuais (escala 2). */
    public record Item(
            UUID insumoId,
            BigDecimal valorConsumo,
            BigDecimal participacaoPct,
            BigDecimal acumuladoPct,
            Classe classe) {}

    /** Classifica as entradas em A/B/C; devolve-as ordenadas por valor de consumo (desc). */
    public List<Item> classificar(List<Entrada> entradas) {
        List<Entrada> ordenadas = entradas.stream()
                .sorted(Comparator.comparing(Entrada::valorConsumo).reversed())
                .toList();

        BigDecimal total = ordenadas.stream()
                .map(Entrada::valorConsumo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Item> itens = new ArrayList<>(ordenadas.size());
        BigDecimal acumulado = BigDecimal.ZERO;
        for (Entrada e : ordenadas) {
            BigDecimal participacao = total.signum() == 0
                    ? BigDecimal.ZERO
                    : e.valorConsumo().multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
            BigDecimal acumuladoAntes = acumulado;
            acumulado = acumulado.add(participacao);

            Classe classe = total.signum() == 0
                    ? Classe.C
                    : acumuladoAntes.compareTo(LIMITE_A) < 0
                        ? Classe.A
                        : acumuladoAntes.compareTo(LIMITE_B) < 0 ? Classe.B : Classe.C;

            itens.add(new Item(e.insumoId(), e.valorConsumo(), participacao,
                    acumulado.min(CEM), classe));
        }
        return itens;
    }
}
