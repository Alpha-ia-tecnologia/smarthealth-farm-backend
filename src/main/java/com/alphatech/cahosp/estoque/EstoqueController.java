package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueDetalheResponse;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueResponse;
import com.alphatech.cahosp.estoque.dto.ResumoEstoqueResponse;
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
 * Posicoes de estoque e KPIs (RF-EST-01/04/05). Leitura para qualquer autenticado.
 */
@RestController
@RequestMapping("/estoque")
@Tag(name = "Estoque", description = "Posicoes de estoque, status e drill-down por lote (RF-EST)")
public class EstoqueController {

    private final EstoqueConsultaService consultaService;

    public EstoqueController(EstoqueConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping
    @Operation(summary = "Lista posicoes de estoque com filtros (unidade, medicamento, status, busca)")
    public ResponseEntity<ApiResponse<List<PosicaoEstoqueResponse>>> listar(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false) StatusEstoque status,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(ApiResponse.lista(
                consultaService.listarPosicoes(unidadeId, medicamentoId, status, busca)));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do estoque (itens criticos, lotes a vencer, lead medio, total)")
    public ResponseEntity<ApiResponse<ResumoEstoqueResponse>> resumo() {
        return ResponseEntity.ok(ApiResponse.ok(consultaService.resumo()));
    }

    @GetMapping("/{medicamentoId}/{unidadeId}")
    @Operation(summary = "Detalha uma posicao: lotes e movimentacoes recentes")
    public ResponseEntity<ApiResponse<PosicaoEstoqueDetalheResponse>> detalhar(
            @PathVariable UUID medicamentoId,
            @PathVariable UUID unidadeId) {
        return ResponseEntity.ok(ApiResponse.ok(consultaService.detalhar(medicamentoId, unidadeId)));
    }
}
