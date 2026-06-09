package com.alphatech.cahosp.unidade;

import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadeRepository extends JpaRepository<Unidade, UUID> {

    Optional<Unidade> findBySiglaIgnoreCase(String sigla);

    boolean existsBySiglaIgnoreCase(String sigla);

    /**
     * Lista unidades aplicando filtros opcionais (qualquer um pode ser {@code null}).
     * A busca casa, ignore-case, em nome OU sigla OU municipio. RF-DAD-06.
     */
    @Query("""
            SELECT u FROM Unidade u
            WHERE (:porte IS NULL OR u.porte = :porte)
              AND (:conectividade IS NULL OR u.conectividade = :conectividade)
              AND (:hub IS NULL OR u.hub = :hub)
              AND (:ativo IS NULL OR u.ativo = :ativo)
              AND (:busca IS NULL
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.municipio) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<Unidade> buscarComFiltros(@Param("porte") Porte porte,
                                   @Param("conectividade") Conectividade conectividade,
                                   @Param("hub") Boolean hub,
                                   @Param("ativo") Boolean ativo,
                                   @Param("busca") String busca,
                                   Sort sort);
}
