package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import com.alphatech.cahosp.recomendacao.dto.CriarRecomendacaoRequest;
import com.alphatech.cahosp.recomendacao.dto.EditarRecomendacaoRequest;
import com.alphatech.cahosp.recomendacao.dto.GeracaoRecomendacoesResponse;
import com.alphatech.cahosp.recomendacao.dto.RecomendacaoResponse;
import com.alphatech.cahosp.recomendacao.dto.ResumoRecomendacoesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
 * Recomendacoes de reposicao e redistribuicao (RF-REC). Leitura para qualquer autenticado;
 * aprovar, executar e regenerar sao acoes de Gestor.
 */
@RestController
@RequestMapping("/recomendacoes")
@Tag(name = "Recomendacoes", description = "Reposicao e redistribuicao dimensionadas por regra (RF-REC)")
public class RecomendacaoController {

    private final RecomendacaoService recomendacaoService;

    public RecomendacaoController(RecomendacaoService recomendacaoService) {
        this.recomendacaoService = recomendacaoService;
    }

    @GetMapping
    @Operation(summary = "Lista recomendacoes, paginadas, com filtros (tipo, status, motor, prioridade, unidade, medicamento, busca)")
    public ResponseEntity<ApiResponse<List<RecomendacaoResponse>>> listar(
            @RequestParam(required = false) TipoRecomendacao tipo,
            @RequestParam(required = false) StatusRecomendacao status,
            @RequestParam(required = false) OrigemMotor origemMotor,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "economiaEstimada", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<RecomendacaoResponse> pagina = recomendacaoService.listar(
                tipo, status, origemMotor, prioridade, unidadeId, medicamentoId, busca, pageable);
        return ResponseEntity.ok(ApiResponse.pagina(pagina.getContent(), pagina.getTotalElements()));
    }

    @GetMapping("/resumo")
    @Operation(summary = "KPIs do painel (pendentes, economia potencial, geradas por IA, taxa de adesao); filtros por unidade/medicamento")
    public ResponseEntity<ApiResponse<ResumoRecomendacoesResponse>> resumo(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId) {
        return ResponseEntity.ok(ApiResponse.ok(recomendacaoService.resumo(unidadeId, medicamentoId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Cria manualmente uma transferencia (redistribuicao) entre unidades (acao de Gestor)")
    public ResponseEntity<ApiResponse<RecomendacaoResponse>> criar(
            @Valid @RequestBody CriarRecomendacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(recomendacaoService.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Edita uma recomendacao pendente: medicamento, unidades e quantidade (acao de Gestor)")
    public ResponseEntity<ApiResponse<RecomendacaoResponse>> editar(
            @PathVariable UUID id, @Valid @RequestBody EditarRecomendacaoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(recomendacaoService.editar(id, request)));
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Aprova uma recomendacao pendente (acao de Gestor)")
    public ResponseEntity<ApiResponse<RecomendacaoResponse>> aprovar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recomendacaoService.aprovar(id)));
    }

    @PostMapping("/{id}/recusar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Recusa (descarta) uma recomendacao pendente (acao de Gestor)")
    public ResponseEntity<ApiResponse<RecomendacaoResponse>> recusar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recomendacaoService.recusar(id)));
    }

    @PostMapping("/{id}/executar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Marca uma recomendacao aprovada como executada (acao de Gestor)")
    public ResponseEntity<ApiResponse<RecomendacaoResponse>> executar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recomendacaoService.executar(id)));
    }

    @PostMapping("/gerar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Regenera as recomendacoes pelo motor de regras (acao de Gestor)")
    public ResponseEntity<ApiResponse<GeracaoRecomendacoesResponse>> gerar() {
        return ResponseEntity.ok(ApiResponse.ok(recomendacaoService.gerar()));
    }
}
