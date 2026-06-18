package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.estoque.LoteRepository;
import com.alphatech.cahosp.estoque.PosicaoEstoqueRepository;
import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import com.alphatech.cahosp.previsao.PrevisaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios do recalculo de indicadores no escopo (RF-IND/RF-DASH): valor atual real de
 * ruptura/vencimento/MAPE e o mock deterministico de compras emergenciais.
 */
@ExtendWith(MockitoExtension.class)
class CalculadoraIndicadorEscopoTest {

    private static final UUID UNI = UUID.randomUUID();
    private static final UUID INS = UUID.randomUUID();

    @Mock private PosicaoEstoqueRepository posicaoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private PrevisaoRepository previsaoRepository;

    @InjectMocks private CalculadoraIndicadorEscopo calc;

    private static IndicadorMeta indicador(String codigo, double atual) {
        return new IndicadorMeta(codigo, "Nome " + codigo, "%",
                new BigDecimal("18.40"), BigDecimal.valueOf(atual), new BigDecimal("11.96"),
                35, true, 0);
    }

    @Test
    @DisplayName("Sem filtro (unidade e insumo nulos) nao ajusta nada")
    void semFiltroNaoAjusta() {
        assertThat(calc.ajustar(indicador("ind-ruptura", 11.2), null, null)).isEmpty();
    }

    @Test
    @DisplayName("Ruptura: essenciais criticos ÷ total de essenciais no escopo")
    void ruptura() {
        when(posicaoRepository.contarEssenciais(UNI, INS)).thenReturn(10L);
        when(posicaoRepository.contarEssenciaisCriticos(UNI, INS)).thenReturn(3L);

        Optional<CalculadoraIndicadorEscopo.Ajuste> ajuste = calc.ajustar(indicador("ind-ruptura", 11.2), UNI, INS);

        assertThat(ajuste).isPresent();
        assertThat(ajuste.get().atual()).isEqualByComparingTo("30.00");
        assertThat(ajuste.get().numeradorAbsoluto()).isEqualByComparingTo("3");
        assertThat(ajuste.get().denominadorAbsoluto()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Ruptura sem essenciais no escopo => sem ajuste (taxa indefinida, mantem edital)")
    void rupturaSemEssenciais() {
        when(posicaoRepository.contarEssenciais(UNI, INS)).thenReturn(0L);
        assertThat(calc.ajustar(indicador("ind-ruptura", 11.2), UNI, INS)).isEmpty();
    }

    @Test
    @DisplayName("Vencimento: lotes vencidos com saldo ÷ total de lotes com saldo")
    void vencimento() {
        when(loteRepository.contarComSaldo(UNI, null)).thenReturn(20L);
        when(loteRepository.contarVencidosComSaldo(any(LocalDate.class), eq(UNI), eq(null))).thenReturn(5L);

        Optional<CalculadoraIndicadorEscopo.Ajuste> ajuste = calc.ajustar(indicador("ind-vencimento", 4.3), UNI, null);

        assertThat(ajuste).isPresent();
        assertThat(ajuste.get().atual()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("MAPE: media das previsoes do escopo, sem lastro absoluto")
    void mape() {
        when(previsaoRepository.mediaMape(null, INS)).thenReturn(new BigDecimal("12.345"));

        Optional<CalculadoraIndicadorEscopo.Ajuste> ajuste = calc.ajustar(indicador("ind-mape", 11.8), null, INS);

        assertThat(ajuste).isPresent();
        assertThat(ajuste.get().atual()).isEqualByComparingTo("12.35");
        assertThat(ajuste.get().numeradorAbsoluto()).isNull();
    }

    @Test
    @DisplayName("MAPE sem previsao no escopo => sem ajuste")
    void mapeSemPrevisao() {
        when(previsaoRepository.mediaMape(UNI, INS)).thenReturn(null);
        assertThat(calc.ajustar(indicador("ind-mape", 11.8), UNI, INS)).isEmpty();
    }

    @Test
    @DisplayName("Compras emergenciais: mock deterministico, varia por escopo dentro de [0.35, 1.05] da rede")
    void emergencialMock() {
        IndicadorMeta ind = new IndicadorMeta("ind-emergencial", "Compras emergenciais", "R$ mil",
                new BigDecimal("1240"), new BigDecimal("812"), new BigDecimal("868"), 30, true, 0);

        Optional<CalculadoraIndicadorEscopo.Ajuste> a1 = calc.ajustar(ind, UNI, INS);
        Optional<CalculadoraIndicadorEscopo.Ajuste> a2 = calc.ajustar(ind, UNI, INS);
        Optional<CalculadoraIndicadorEscopo.Ajuste> outro = calc.ajustar(ind, UUID.randomUUID(), INS);

        assertThat(a1).isPresent();
        // Deterministico: mesmo escopo => mesmo valor.
        assertThat(a1.get().atual()).isEqualByComparingTo(a2.get().atual());
        // Dentro da faixa esperada (0.35..1.05 de 812).
        assertThat(a1.get().atual()).isBetween(new BigDecimal("284.20"), new BigDecimal("852.60"));
        // Escopo diferente => valor diferente (varia com o filtro).
        assertThat(a1.get().atual()).isNotEqualByComparingTo(outro.get().atual());
    }

    @Test
    @DisplayName("Indicador sem dado de escopo (ex.: ind-ressuprimento) nao e ajustado")
    void indicadorNaoAjustavel() {
        assertThat(calc.ajustar(indicador("ind-ressuprimento", 13.7), UNI, INS)).isEmpty();
    }
}
