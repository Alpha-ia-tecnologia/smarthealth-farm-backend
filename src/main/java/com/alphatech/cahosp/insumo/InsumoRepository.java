package com.alphatech.cahosp.insumo;

import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsumoRepository extends JpaRepository<Insumo, UUID> {

    Optional<Insumo> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * Lista insumos aplicando filtros opcionais. A busca casa, ignore-case, em
     * nome OU codigo OU apresentacao. RF-DAD-06.
     */
    @Query("""
            SELECT m FROM Insumo m
            WHERE (:categoria IS NULL OR m.categoria = :categoria)
              AND (:criticidade IS NULL OR m.criticidade = :criticidade)
              AND (:essencial IS NULL OR m.essencial = :essencial)
              AND (:ativo IS NULL OR m.ativo = :ativo)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(m.apresentacao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<Insumo> buscarComFiltros(@Param("categoria") CategoriaInsumo categoria,
                                       @Param("criticidade") Criticidade criticidade,
                                       @Param("essencial") Boolean essencial,
                                       @Param("ativo") Boolean ativo,
                                       @Param("busca") String busca,
                                       Sort sort);

    /**
     * Variante do {@link #buscarComFiltros} restrita aos insumos que possuem
     * posicao de estoque na unidade informada — usada pelo filtro dependente de
     * insumo no front (seleciona uma unidade -> lista so os seus itens). RF-DAD-06.
     */
    @Query("""
            SELECT DISTINCT m FROM Insumo m
            JOIN PosicaoEstoque p ON p.insumo = m
            WHERE p.unidade.id = :unidadeId
              AND (:categoria IS NULL OR m.categoria = :categoria)
              AND (:criticidade IS NULL OR m.criticidade = :criticidade)
              AND (:essencial IS NULL OR m.essencial = :essencial)
              AND (:ativo IS NULL OR m.ativo = :ativo)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(m.apresentacao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<Insumo> buscarPorUnidadeComFiltros(@Param("unidadeId") UUID unidadeId,
                                                 @Param("categoria") CategoriaInsumo categoria,
                                                 @Param("criticidade") Criticidade criticidade,
                                                 @Param("essencial") Boolean essencial,
                                                 @Param("ativo") Boolean ativo,
                                                 @Param("busca") String busca,
                                                 Sort sort);
}
