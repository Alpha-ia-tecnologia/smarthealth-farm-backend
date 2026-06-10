package com.alphatech.cahosp.integracao;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.integracao.dto.IntegracaoApiResponse;
import com.alphatech.cahosp.integracao.dto.ProvedorIaResponse;
import com.alphatech.cahosp.integracao.dto.ResumoIntegracaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Integracao com os sistemas da EMSERH (RF-INT): conexoes/APIs, AI Gateway e KPIs. Modulo de
 * governanca, somente leitura — disponivel para qualquer autenticado.
 */
@RestController
@RequestMapping("/integracoes")
@Tag(name = "Integracao", description = "APIs EMSERH, resiliencia de conectividade e AI Gateway (RF-INT)")
public class IntegracaoController {

    private final IntegracaoService integracaoService;

    public IntegracaoController(IntegracaoService integracaoService) {
        this.integracaoService = integracaoService;
    }

    @GetMapping
    @Operation(summary = "Lista as integracoes com status, latencia, modo e buffer offline")
    public ResponseEntity<ApiResponse<List<IntegracaoApiResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.lista(integracaoService.listarIntegracoes()));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel (operacionais, latencia media, buffer, provedores de IA)")
    public ResponseEntity<ApiResponse<ResumoIntegracaoResponse>> resumo() {
        return ResponseEntity.ok(ApiResponse.ok(integracaoService.resumo()));
    }

    @GetMapping("/provedores-ia")
    @Operation(summary = "Lista os provedores de IA do AI Gateway (papel, custo, anonimizacao)")
    public ResponseEntity<ApiResponse<List<ProvedorIaResponse>>> listarProvedoresIa() {
        return ResponseEntity.ok(ApiResponse.lista(integracaoService.listarProvedoresIa()));
    }
}
