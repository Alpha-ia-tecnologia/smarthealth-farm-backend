package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dto.CurvaAbcResponse;
import com.alphatech.cahosp.insumo.InsumoRepository;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Monta a Curva ABC dos insumos por valor de consumo (RF-EST): para cada insumo, soma o consumo
 * medio diario na rede e multiplica pelo custo unitario; a {@link CalculadoraCurvaAbc} classifica
 * em A/B/C. Modulo de leitura (controller fino · regra na calculadora · este servico orquestra).
 */
@Service
public class CurvaAbcService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final PosicaoEstoqueRepository posicaoRepository;
    private final InsumoRepository insumoRepository;
    private final CalculadoraCurvaAbc calculadora;

    public CurvaAbcService(PosicaoEstoqueRepository posicaoRepository,
                           InsumoRepository insumoRepository,
                           CalculadoraCurvaAbc calculadora) {
        this.posicaoRepository = posicaoRepository;
        this.insumoRepository = insumoRepository;
        this.calculadora = calculadora;
    }

    /** Dado agregado de um insumo antes da classificacao. */
    private record Dado(Insumo insumo, long consumo, BigDecimal valor) {}

    @Transactional(readOnly = true)
    public CurvaAbcResponse calcular() {
        Map<UUID, Insumo> insumos = new HashMap<>();
        for (Insumo i : insumoRepository.findAll()) {
            insumos.put(i.getId(), i);
        }

        Map<UUID, Dado> dados = new LinkedHashMap<>();
        List<CalculadoraCurvaAbc.Entrada> entradas = new ArrayList<>();
        for (Object[] linha : posicaoRepository.somarConsumoPorInsumo()) {
            UUID insumoId = (UUID) linha[0];
            long consumo = ((Number) linha[1]).longValue();
            Insumo insumo = insumos.get(insumoId);
            if (insumo == null) {
                continue;
            }
            BigDecimal custo = insumo.getCustoUnitario() == null ? BigDecimal.ZERO : insumo.getCustoUnitario();
            BigDecimal valor = custo.multiply(BigDecimal.valueOf(consumo));
            dados.put(insumoId, new Dado(insumo, consumo, valor));
            entradas.add(new CalculadoraCurvaAbc.Entrada(insumoId, valor));
        }

        List<CurvaAbcResponse.Item> itens = calculadora.classificar(entradas).stream()
                .map(c -> {
                    Dado d = dados.get(c.insumoId());
                    return new CurvaAbcResponse.Item(
                            d.insumo().getId(),
                            d.insumo().getCodigo(),
                            d.insumo().getNome(),
                            d.consumo(),
                            d.insumo().getCustoUnitario(),
                            c.valorConsumo(),
                            c.participacaoPct(),
                            c.acumuladoPct(),
                            c.classe().name());
                })
                .toList();

        return new CurvaAbcResponse(itens, resumir(itens));
    }

    /** Consolida itens/valor e os percentuais por classe A/B/C. */
    private List<CurvaAbcResponse.ResumoClasse> resumir(List<CurvaAbcResponse.Item> itens) {
        BigDecimal valorTotal = itens.stream()
                .map(CurvaAbcResponse.Item::valorConsumo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItens = itens.size();

        List<CurvaAbcResponse.ResumoClasse> resumo = new ArrayList<>();
        for (String classe : List.of("A", "B", "C")) {
            List<CurvaAbcResponse.Item> doGrupo = itens.stream()
                    .filter(i -> i.classe().equals(classe))
                    .toList();
            BigDecimal valor = doGrupo.stream()
                    .map(CurvaAbcResponse.Item::valorConsumo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal itensPct = totalItens == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(doGrupo.size()).multiply(CEM)
                            .divide(BigDecimal.valueOf(totalItens), 2, RoundingMode.HALF_UP);
            BigDecimal valorPct = valorTotal.signum() == 0
                    ? BigDecimal.ZERO
                    : valor.multiply(CEM).divide(valorTotal, 2, RoundingMode.HALF_UP);
            resumo.add(new CurvaAbcResponse.ResumoClasse(classe, doGrupo.size(), valor, itensPct, valorPct));
        }
        return resumo;
    }
}
