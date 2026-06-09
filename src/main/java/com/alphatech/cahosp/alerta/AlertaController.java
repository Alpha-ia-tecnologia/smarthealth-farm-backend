package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.alerta.dto.AlertaResponse;
import com.alphatech.cahosp.alerta.dto.AtualizarStatusAlertaRequest;
import com.alphatech.cahosp.alerta.dto.GeracaoAlertasResponse;
import com.alphatech.cahosp.alerta.dto.ResumoAlertasResponse;
import com.alphatech.cahosp.comum.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Alertas operacionais (RF-ALE). Leitura e tratamento para qualquer autenticado (operacao do dia
 * a dia); a regeneracao pelo motor de regras e acao de Gestor.
 */
@RestController
@RequestMapping("/alertas")
@Tag(name = "Alertas", description = "Alertas de desabastecimento e vencimento, gerados por regra (RF-ALE)")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    @Operation(summary = "Lista alertas com filtros (tipo, severidade, status, unidade, medicamento, busca)")
    public ResponseEntity<ApiResponse<List<AlertaResponse>>> listar(
            @RequestParam(required = false) TipoAlerta tipo,
            @RequestParam(required = false) Severidade severidade,
            @RequestParam(required = false) StatusAlerta status,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(ApiResponse.lista(
                alertaService.listar(tipo, severidade, status, unidadeId, medicamentoId, busca)));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel de alertas (abertos, desabastecimento, vencimento, tratados)")
    public ResponseEntity<ApiResponse<ResumoAlertasResponse>> resumo() {
        return ResponseEntity.ok(ApiResponse.ok(alertaService.resumo()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Trata um alerta: muda o status (Aberto -> Em tratamento -> Resolvido)")
    public ResponseEntity<ApiResponse<AlertaResponse>> atualizarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarStatusAlertaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(alertaService.atualizarStatus(id, request.status())));
    }

    @PostMapping("/gerar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Regenera os alertas pelo motor de regras (acao de Gestor)")
    public ResponseEntity<ApiResponse<GeracaoAlertasResponse>> gerar() {
        return ResponseEntity.ok(ApiResponse.ok(alertaService.gerar()));
    }
}
