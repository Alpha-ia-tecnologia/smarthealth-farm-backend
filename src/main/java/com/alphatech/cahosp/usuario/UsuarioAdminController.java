package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.seguranca.UsuarioAutenticado;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import com.alphatech.cahosp.usuario.dto.AlterarStatusRequest;
import com.alphatech.cahosp.usuario.dto.AtualizarUsuarioRequest;
import com.alphatech.cahosp.usuario.dto.CriarUsuarioRequest;
import com.alphatech.cahosp.usuario.dto.RedefinirSenhaRequest;
import com.alphatech.cahosp.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * Administracao de usuarios (RF-ADM-01). Exclusivo do perfil TI (RBAC). Controller fino:
 * delega toda a regra ao {@link UsuarioAdminService}. Nao expoe a entidade — sempre {@link UsuarioResponse}.
 */
@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('TI')")
@Tag(name = "Administracao de usuarios", description = "CRUD de usuarios (perfil TI) — RF-ADM-01")
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    public UsuarioAdminController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios com filtros opcionais (perfil, ativo, busca)")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listar(
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(ApiResponse.lista(usuarioAdminService.listar(perfil, ativo, busca)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um usuario por id")
    public ResponseEntity<ApiResponse<UsuarioResponse>> detalhar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Cria um usuario (senha gravada com BCrypt)")
    public ResponseEntity<ApiResponse<UsuarioResponse>> criar(
            @Valid @RequestBody CriarUsuarioRequest request) {
        UsuarioResponse criado = usuarioAdminService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(criado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome, e-mail e perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarUsuarioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.atualizar(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa ou desativa um usuario (nao permite auto-desativacao)")
    public ResponseEntity<ApiResponse<UsuarioResponse>> alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlterarStatusRequest request,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        UsuarioResponse atualizado = usuarioAdminService.alterarStatus(
                id, request.ativo(), autenticado.getUsuario().getId());
        return ResponseEntity.ok(ApiResponse.ok(atualizado));
    }

    @PutMapping("/{id}/senha")
    @Operation(summary = "Redefine a senha do usuario (BCrypt)")
    public ResponseEntity<ApiResponse<String>> redefinirSenha(
            @PathVariable UUID id,
            @Valid @RequestBody RedefinirSenhaRequest request) {
        usuarioAdminService.redefinirSenha(id, request.novaSenha());
        return ResponseEntity.ok(ApiResponse.ok("Senha redefinida com sucesso."));
    }
}
