package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dto.LogAuditoriaResponse;
import com.alphatech.cahosp.seguranca.auditoria.dto.ResumoAuditoriaResponse;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trilha de auditoria e conformidade LGPD (RF-SEG-01/02/03/05). Modulo de governanca,
 * <strong>somente leitura</strong>.
 *
 * <p>RBAC: a revisao de auditoria e atividade de governanca/seguranca — restrita a {@code GESTOR}
 * e {@code TI} (a matriz de acesso do front atribui auditoria de dados sensiveis ao TI e os paineis
 * de governanca ao Gestor); o {@code OPERADOR} nao tem acesso.
 */
@RestController
@RequestMapping("/seguranca/auditoria")
@PreAuthorize("hasAnyRole('GESTOR', 'TI')")
@Tag(name = "Seguranca", description = "Trilha de auditoria e conformidade LGPD (RF-SEG)")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Lista a trilha de auditoria com filtros (categoria, perfil, IA, busca)")
    public ResponseEntity<ApiResponse<List<LogAuditoriaResponse>>> listar(
            @RequestParam(required = false) CategoriaAuditoria categoria,
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) Boolean assistidoPorIA,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(ApiResponse.lista(
                auditoriaService.listar(categoria, perfil, assistidoPorIA, busca)));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel: eventos, assistidos por IA, com base legal e ultima atividade")
    public ResponseEntity<ApiResponse<ResumoAuditoriaResponse>> resumo() {
        return ResponseEntity.ok(ApiResponse.ok(auditoriaService.resumo()));
    }
}
