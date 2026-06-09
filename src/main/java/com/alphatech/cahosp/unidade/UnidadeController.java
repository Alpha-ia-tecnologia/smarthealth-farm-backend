package com.alphatech.cahosp.unidade;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dto.AlterarStatusUnidadeRequest;
import com.alphatech.cahosp.unidade.dto.AtualizarUnidadeRequest;
import com.alphatech.cahosp.unidade.dto.CriarUnidadeRequest;
import com.alphatech.cahosp.unidade.dto.UnidadeResponse;
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
 * Catalogo de unidades (RF-DAD-06). Leitura disponivel para qualquer usuario autenticado;
 * escrita (criar/atualizar/alterar status) restrita ao perfil TI.
 */
@RestController
@RequestMapping("/unidades")
@Tag(name = "Catalogo de unidades", description = "Unidades da rede EMSERH (RF-DAD-06)")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping
    @Operation(summary = "Lista unidades com filtros opcionais (porte, conectividade, hub, ativo, busca)")
    public ResponseEntity<ApiResponse<List<UnidadeResponse>>> listar(
            @RequestParam(required = false) Porte porte,
            @RequestParam(required = false) Conectividade conectividade,
            @RequestParam(required = false) Boolean hub,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(ApiResponse.lista(
                unidadeService.listar(porte, conectividade, hub, ativo, busca)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma unidade por id")
    public ResponseEntity<ApiResponse<UnidadeResponse>> detalhar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(unidadeService.buscarPorId(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Cria uma unidade (perfil TI)")
    public ResponseEntity<ApiResponse<UnidadeResponse>> criar(
            @Valid @RequestBody CriarUnidadeRequest request) {
        UnidadeResponse criada = unidadeService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(criada));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Atualiza uma unidade (perfil TI)")
    public ResponseEntity<ApiResponse<UnidadeResponse>> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarUnidadeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(unidadeService.atualizar(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Ativa ou desativa uma unidade (perfil TI)")
    public ResponseEntity<ApiResponse<UnidadeResponse>> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusUnidadeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(unidadeService.alterarStatus(id, request.ativo())));
    }
}
