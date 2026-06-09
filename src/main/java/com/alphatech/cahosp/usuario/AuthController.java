package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.comum.ApiResponse;
import com.alphatech.cahosp.seguranca.UsuarioAutenticado;
import com.alphatech.cahosp.usuario.dto.LoginRequest;
import com.alphatech.cahosp.usuario.dto.LoginResponse;
import com.alphatech.cahosp.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticacao (RF-SEG). Login por e-mail, JWT stateless.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Login, usuario atual e logout (JWT)")
public class AuthController {

    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha e devolve o token JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(autenticacaoService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuario autenticado (valida o JWT)")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(ApiResponse.ok(UsuarioResponse.de(autenticado.getUsuario())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout stateless — o cliente descarta o token")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Logout efetuado. Descarte o token no cliente."));
    }
}
