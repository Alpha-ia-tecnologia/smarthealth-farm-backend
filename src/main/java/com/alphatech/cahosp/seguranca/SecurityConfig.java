package com.alphatech.cahosp.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuracao de seguranca: stateless + JWT + RBAC por perfil. Senhas com BCrypt.
 * Erros 401/403 saem no envelope padrao da API. RF-SEG.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/auth/login",
            "/auth/logout",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetailsService usuarioDetailsService;
    private final EntryPointNaoAutenticado entryPointNaoAutenticado;
    private final HandlerAcessoNegado handlerAcessoNegado;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          UsuarioDetailsService usuarioDetailsService,
                          EntryPointNaoAutenticado entryPointNaoAutenticado,
                          HandlerAcessoNegado handlerAcessoNegado,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.usuarioDetailsService = usuarioDetailsService;
        this.entryPointNaoAutenticado = entryPointNaoAutenticado;
        this.handlerAcessoNegado = handlerAcessoNegado;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPointNaoAutenticado)
                        .accessDeniedHandler(handlerAcessoNegado))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Hierarquia de papeis (RBAC). O {@code ADMIN} e superusuario: herda Gestor e TI; ambos herdam
     * Operador. Assim qualquer {@code @PreAuthorize("hasRole('GESTOR')")}/{@code 'TI'} ja aceita um
     * Admin sem checagem caso a caso. RF-ADM / RF-SEG.
     */
    static RoleHierarchy construirHierarquiaPapeis() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("GESTOR", "TI")
                .role("GESTOR").implies("OPERADOR")
                .role("TI").implies("OPERADOR")
                .build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return construirHierarquiaPapeis();
    }

    /** Faz o {@code @PreAuthorize} respeitar a hierarquia de papeis acima (method security). */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
