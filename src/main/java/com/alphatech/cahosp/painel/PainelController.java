package com.alphatech.cahosp.painel;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.painel.dto.PainelGerencialResponse;
import com.alphatech.cahosp.painel.dto.PainelOperacionalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Agregacoes de dashboard (RF-DASH-01/02). Modulo somente leitura — disponivel para qualquer
 * usuario autenticado (Operador, Gestor e TI).
 */
@RestController
@RequestMapping("/painel")
@Tag(name = "Painel", description = "Dashboard gerencial e painel operacional (RF-DASH)")
public class PainelController {

    private final PainelService painelService;

    public PainelController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping
    @Operation(summary = "Dashboard gerencial: totais, cobertura, serie agregada, alertas e recomendacoes "
            + "(filtro opcional por unidade; cobertura por unidade permanece da rede inteira)")
    public ResponseEntity<ApiResponse<PainelGerencialResponse>> dashboard(
            @RequestParam(required = false) UUID unidadeId) {
        return ResponseEntity.ok(ApiResponse.ok(painelService.dashboard(unidadeId)));
    }

    @GetMapping("/operacional")
    @Operation(summary = "Painel operacional: situacao por unidade, fila de alertas e recomendacoes em aberto "
            + "(filtros opcionais por unidade e medicamento)")
    public ResponseEntity<ApiResponse<PainelOperacionalResponse>> operacional(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId) {
        return ResponseEntity.ok(ApiResponse.ok(painelService.operacional(unidadeId, medicamentoId)));
    }
}
