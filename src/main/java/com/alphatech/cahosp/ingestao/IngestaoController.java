package com.alphatech.cahosp.ingestao;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.ingestao.dto.FonteDadoResponse;
import com.alphatech.cahosp.ingestao.dto.QualidadeFamiliaResponse;
import com.alphatech.cahosp.ingestao.dto.ResumoIngestaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ingestao e qualidade de dados (RF-DAD). Modulo de governanca, somente leitura — disponivel
 * para qualquer autenticado.
 */
@RestController
@RequestMapping("/ingestao")
@Tag(name = "Ingestao", description = "Fontes de dados e qualidade da base historica (RF-DAD)")
public class IngestaoController {

    private final IngestaoService ingestaoService;

    public IngestaoController(IngestaoService ingestaoService) {
        this.ingestaoService = ingestaoService;
    }

    @GetMapping("/fontes")
    @Operation(summary = "Lista as fontes de dados com status, volume, qualidade e procedencia")
    public ResponseEntity<ApiResponse<List<FonteDadoResponse>>> listarFontes() {
        return ResponseEntity.ok(ApiResponse.lista(ingestaoService.listarFontes()));
    }

    @GetMapping("/qualidade")
    @Operation(summary = "Lista maturidade e qualidade por familia terapeutica")
    public ResponseEntity<ApiResponse<List<QualidadeFamiliaResponse>>> listarQualidade() {
        return ResponseEntity.ok(ApiResponse.lista(ingestaoService.listarQualidade()));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel (registros, fontes sincronizadas, qualidade media, LGPD)")
    public ResponseEntity<ApiResponse<ResumoIngestaoResponse>> resumo() {
        return ResponseEntity.ok(ApiResponse.ok(ingestaoService.resumo()));
    }
}
