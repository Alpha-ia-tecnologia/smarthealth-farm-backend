package com.alphatech.cahosp.ia;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.ia.dto.ChatRequest;
import com.alphatech.cahosp.ia.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI Gateway (RF-INT-06 / RF-SEG-04): proxy para provedores de IA com anonimizacao e modo demo.
 * Disponivel para qualquer autenticado.
 */
@RestController
@RequestMapping("/ia")
@Tag(name = "IA", description = "AI Gateway: chat com provedores externos, anonimizado e com modo demo (RF-INT-06)")
public class IaController {

    private final GatewayIaService gatewayIaService;

    public IaController(GatewayIaService gatewayIaService) {
        this.gatewayIaService = gatewayIaService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversa com a IA (anonimiza antes do envio; cai para modo demo sem provedor)")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(gatewayIaService.conversar(request.mensagens())));
    }
}
