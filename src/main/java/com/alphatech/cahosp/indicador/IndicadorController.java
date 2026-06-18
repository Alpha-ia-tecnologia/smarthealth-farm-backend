package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.indicador.dto.IndicadorResponse;
import com.alphatech.cahosp.indicador.dto.ResumoIndicadoresResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Indicadores de desempenho do projeto frente as metas do edital (RF-IND). Modulo de governanca,
 * somente leitura — disponivel para qualquer autenticado.
 */
@RestController
@RequestMapping("/indicadores")
@Tag(name = "Indicadores", description = "Metas do projeto vs. linha de base e historico (RF-IND)")
public class IndicadorController {

    private final IndicadorService indicadorService;

    public IndicadorController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    @GetMapping
    @Operation(summary = "Lista os indicadores com historico, progresso, meta atingida e variacao. "
            + "Filtros opcionais por unidade/insumo recalculam o valor atual no escopo "
            + "(baseline/meta/historico seguem do edital)")
    public ResponseEntity<ApiResponse<List<IndicadorResponse>>> listar(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID insumoId) {
        return ResponseEntity.ok(ApiResponse.lista(indicadorService.listar(unidadeId, insumoId)));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel (total, metas atingidas, em progresso)")
    public ResponseEntity<ApiResponse<ResumoIndicadoresResponse>> resumo() {
        return ResponseEntity.ok(ApiResponse.ok(indicadorService.resumo()));
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Detalha um indicador pelo codigo de negocio (ex.: ind-ruptura)")
    public ResponseEntity<ApiResponse<IndicadorResponse>> detalhar(@PathVariable String codigo) {
        return ResponseEntity.ok(ApiResponse.ok(indicadorService.detalhar(codigo)));
    }
}
