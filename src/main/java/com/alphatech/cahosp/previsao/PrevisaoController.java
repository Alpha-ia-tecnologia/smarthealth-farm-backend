package com.alphatech.cahosp.previsao;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.previsao.dto.PainelPrevisaoResponse;
import com.alphatech.cahosp.previsao.dto.PrevisaoDetalheResponse;
import com.alphatech.cahosp.previsao.dto.PrevisaoResumoResponse;
import com.alphatech.cahosp.previsao.dto.RecalibracaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Previsao de demanda (RF-PRV). Leitura para qualquer autenticado; recalibrar e acao de Gestor.
 */
@RestController
@RequestMapping("/previsoes")
@Tag(name = "Previsao de demanda", description = "Series, assertividade (MAPE) e drift (RF-PRV)")
public class PrevisaoController {

    private final PrevisaoService previsaoService;

    public PrevisaoController(PrevisaoService previsaoService) {
        this.previsaoService = previsaoService;
    }

    @GetMapping
    @Operation(summary = "Lista previsoes, paginadas, com filtros (unidade, medicamento, drift, busca)")
    public ResponseEntity<ApiResponse<List<PrevisaoResumoResponse>>> listar(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false) Drift drift,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "medicamento.nome") Pageable pageable) {
        Page<PrevisaoResumoResponse> pagina = previsaoService.listar(
                unidadeId, medicamentoId, drift, busca, pageable);
        return ResponseEntity.ok(ApiResponse.pagina(pagina.getContent(), pagina.getTotalElements()));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel de previsao (MAPE medio, criticos na meta, drift); filtros por unidade/medicamento")
    public ResponseEntity<ApiResponse<PainelPrevisaoResponse>> resumo(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId) {
        return ResponseEntity.ok(ApiResponse.ok(previsaoService.resumo(unidadeId, medicamentoId)));
    }

    @GetMapping("/{medicamentoId}/{unidadeId}")
    @Operation(summary = "Detalha uma previsao com a serie temporal completa")
    public ResponseEntity<ApiResponse<PrevisaoDetalheResponse>> detalhar(
            @PathVariable UUID medicamentoId,
            @PathVariable UUID unidadeId) {
        return ResponseEntity.ok(ApiResponse.ok(previsaoService.detalhar(medicamentoId, unidadeId)));
    }

    @PostMapping("/recalibrar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Recalibra as previsoes (acao de Gestor) — estabiliza drift e marca a data")
    public ResponseEntity<ApiResponse<RecalibracaoResponse>> recalibrar() {
        return ResponseEntity.ok(ApiResponse.ok(previsaoService.recalibrar()));
    }
}
