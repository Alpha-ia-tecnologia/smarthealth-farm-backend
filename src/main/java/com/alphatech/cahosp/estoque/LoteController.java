package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.estoque.dto.CriarLoteRequest;
import com.alphatech.cahosp.estoque.dto.LoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Lotes (RF-EST-02/03): consulta com controle de validade e entrada de novos lotes.
 * Leitura e escrita para qualquer autenticado (operacao do estoque).
 */
@RestController
@RequestMapping("/lotes")
@Tag(name = "Lotes", description = "Rastreabilidade por lote e controle de validade (RF-EST-02/03)")
public class LoteController {

    private final EstoqueConsultaService consultaService;
    private final EstoqueMovimentacaoService movimentacaoService;

    public LoteController(EstoqueConsultaService consultaService,
                          EstoqueMovimentacaoService movimentacaoService) {
        this.consultaService = consultaService;
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping
    @Operation(summary = "Lista lotes, paginados (filtros: unidade, medicamento, comSaldo, validadeAteDias)")
    public ResponseEntity<ApiResponse<List<LoteResponse>>> listar(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false, defaultValue = "false") boolean comSaldo,
            @RequestParam(required = false) Integer validadeAteDias,
            @PageableDefault(size = 10, sort = "validade") Pageable pageable) {
        Page<LoteResponse> pagina =
                consultaService.listarLotes(unidadeId, medicamentoId, comSaldo, validadeAteDias, pageable);
        return ResponseEntity.ok(ApiResponse.pagina(pagina.getContent(), pagina.getTotalElements()));
    }

    @PostMapping
    @Operation(summary = "Cria um lote e registra a Entrada inicial no livro-razao")
    public ResponseEntity<ApiResponse<LoteResponse>> criar(@Valid @RequestBody CriarLoteRequest request) {
        LoteResponse criado = movimentacaoService.criarLote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(criado));
    }
}
