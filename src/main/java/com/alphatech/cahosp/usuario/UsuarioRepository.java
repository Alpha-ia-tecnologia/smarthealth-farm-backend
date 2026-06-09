package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Lista usuarios aplicando filtros opcionais (qualquer um pode ser {@code null}).
     * A busca casa, ignore-case, em nome OU e-mail. RF-ADM-01.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:perfil IS NULL OR u.perfil = :perfil)
              AND (:ativo IS NULL OR u.ativo = :ativo)
              AND (:busca IS NULL
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<Usuario> buscarComFiltros(@Param("perfil") Perfil perfil,
                                   @Param("ativo") Boolean ativo,
                                   @Param("busca") String busca,
                                   Sort sort);
}
