package com.alphatech.cahosp.medicamento;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import com.alphatech.cahosp.medicamento.dto.AlterarStatusMedicamentoRequest;
import com.alphatech.cahosp.medicamento.dto.AtualizarMedicamentoRequest;
import com.alphatech.cahosp.medicamento.dto.CriarMedicamentoRequest;
import com.alphatech.cahosp.medicamento.dto.MedicamentoResponse;
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
 * Catalogo de medicamentos (RF-DAD-06). Leitura para qualquer autenticado;
 * escrita restrita ao perfil TI.
 */
@RestController
@RequestMapping("/medicamentos")
@Tag(name = "Catalogo de medicamentos", description = "Medicamentos e insumos (RF-DAD-06)")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    public MedicamentoController(MedicamentoService medicamentoService) {
        this.medicamentoService = medicamentoService;
    }

    @GetMapping
    @Operation(summary = "Lista medicamentos com filtros opcionais (familia, criticidade, essencial, ativo, busca)")
    public ResponseEntity<ApiResponse<List<MedicamentoResponse>>> listar(
            @RequestParam(required = false) FamiliaTerapeutica familia,
            @RequestParam(required = false) Criticidade criticidade,
            @RequestParam(required = false) Boolean essencial,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(ApiResponse.lista(
                medicamentoService.listar(familia, criticidade, essencial, ativo, busca)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um medicamento por id")
    public ResponseEntity<ApiResponse<MedicamentoResponse>> detalhar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(medicamentoService.buscarPorId(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Cria um medicamento (perfil TI)")
    public ResponseEntity<ApiResponse<MedicamentoResponse>> criar(
            @Valid @RequestBody CriarMedicamentoRequest request) {
        MedicamentoResponse criado = medicamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(criado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Atualiza um medicamento (perfil TI)")
    public ResponseEntity<ApiResponse<MedicamentoResponse>> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarMedicamentoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(medicamentoService.atualizar(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TI')")
    @Operation(summary = "Ativa ou desativa um medicamento (perfil TI)")
    public ResponseEntity<ApiResponse<MedicamentoResponse>> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusMedicamentoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                medicamentoService.alterarStatus(id, request.ativo())));
    }
}
