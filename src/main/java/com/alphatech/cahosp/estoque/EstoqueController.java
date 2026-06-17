package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueDetalheResponse;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueResponse;
import com.alphatech.cahosp.estoque.dto.ResumoEstoqueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    @Operation(summary = "Lista posicoes de estoque, paginadas, com filtros (unidade, insumo, status, busca)")
    public ResponseEntity<ApiResponse<List<PosicaoEstoqueResponse>>> listar(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID insumoId,
            @RequestParam(required = false) StatusEstoque status,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "insumo.nome") Pageable pageable) {
        Page<PosicaoEstoqueResponse> pagina =
                consultaService.listarPosicoes(unidadeId, insumoId, status, busca, pageable);
        return ResponseEntity.ok(ApiResponse.pagina(pagina.getContent(), pagina.getTotalElements()));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do estoque (itens criticos, lotes a vencer, lead medio, total); filtros por unidade/insumo")
    public ResponseEntity<ApiResponse<ResumoEstoqueResponse>> resumo(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID insumoId) {
        return ResponseEntity.ok(ApiResponse.ok(consultaService.resumo(unidadeId, insumoId)));
    }

    @GetMapping("/{insumoId}/{unidadeId}")
    @Operation(summary = "Detalha uma posicao: lotes e movimentacoes recentes")
    public ResponseEntity<ApiResponse<PosicaoEstoqueDetalheResponse>> detalhar(
            @PathVariable UUID insumoId,
            @PathVariable UUID unidadeId) {
        return ResponseEntity.ok(ApiResponse.ok(consultaService.detalhar(insumoId, unidadeId)));
    }
}
