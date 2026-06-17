package com.alphatech.cahosp.insumo;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dto.AlterarStatusInsumoRequest;
import com.alphatech.cahosp.insumo.dto.AtualizarInsumoRequest;
import com.alphatech.cahosp.insumo.dto.CriarInsumoRequest;
import com.alphatech.cahosp.insumo.dto.InsumoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Catalogo de insumos (RF-DAD-06). Leitura para qualquer autenticado;
 * escrita restrita ao perfil TI.
 */
@RestController
@RequestMapping("/insumos")
@Tag(name = "Catalogo de insumos", description = "Insumos e insumos (RF-DAD-06)")
public class InsumoController {

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @GetMapping
    @Operation(summary = "Lista insumos com filtros opcionais (categoria, criticidade, essencial, ativo, busca, unidade)")
    public ResponseEntity<ApiResponse<List<InsumoResponse>>> listar(
            @RequestParam(required = false) CategoriaInsumo categoria,
            @RequestParam(required = false) Criticidade criticidade,
            @RequestParam(required = false) Boolean essencial,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) UUID unidadeId) {
        return ResponseEntity.ok(ApiResponse.lista(
                insumoService.listar(categoria, criticidade, essencial, ativo, busca, unidadeId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um insumo por id")
    public ResponseEntity<ApiResponse<InsumoResponse>> detalhar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(insumoService.buscarPorId(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Cria um insumo (perfil TI)")
    public ResponseEntity<ApiResponse<InsumoResponse>> criar(
            @Valid @RequestBody CriarInsumoRequest request) {
        InsumoResponse criado = insumoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(criado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Atualiza um insumo (perfil TI)")
    public ResponseEntity<ApiResponse<InsumoResponse>> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarInsumoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(insumoService.atualizar(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Ativa ou desativa um insumo (perfil TI)")
    public ResponseEntity<ApiResponse<InsumoResponse>> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusInsumoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                insumoService.alterarStatus(id, request.ativo())));
    }
}
