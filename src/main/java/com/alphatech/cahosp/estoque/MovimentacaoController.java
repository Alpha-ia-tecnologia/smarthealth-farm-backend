package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import com.alphatech.cahosp.estoque.dto.MovimentacaoResponse;
import com.alphatech.cahosp.estoque.dto.RegistrarMovimentacaoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * Movimentacoes — o livro-razao do estoque (RF-EST-06). Append-only: so consulta e registro.
 */
@RestController
@RequestMapping("/movimentacoes")
@Tag(name = "Movimentacoes", description = "Livro-razao do estoque, imutavel (RF-EST-06)")
public class MovimentacaoController {

    private final EstoqueConsultaService consultaService;
    private final EstoqueMovimentacaoService movimentacaoService;

    public MovimentacaoController(EstoqueConsultaService consultaService,
                                  EstoqueMovimentacaoService movimentacaoService) {
        this.consultaService = consultaService;
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping
    @Operation(summary = "Lista movimentacoes (filtros: medicamento, unidade, lote, tipo)")
    public ResponseEntity<ApiResponse<List<MovimentacaoResponse>>> listar(
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID loteId,
            @RequestParam(required = false) TipoMovimentacao tipo) {
        return ResponseEntity.ok(ApiResponse.lista(
                consultaService.listarMovimentacoes(medicamentoId, unidadeId, loteId, tipo)));
    }

    @PostMapping
    @Operation(summary = "Registra um lancamento sobre um lote (ajusta saldo e posicao)")
    public ResponseEntity<ApiResponse<MovimentacaoResponse>> registrar(
            @Valid @RequestBody RegistrarMovimentacaoRequest request) {
        MovimentacaoResponse registrada = movimentacaoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(registrada));
    }
}
