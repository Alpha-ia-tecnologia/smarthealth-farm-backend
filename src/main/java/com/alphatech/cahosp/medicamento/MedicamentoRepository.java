package com.alphatech.cahosp.medicamento;

import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicamentoRepository extends JpaRepository<Medicamento, UUID> {

    Optional<Medicamento> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * Lista medicamentos aplicando filtros opcionais. A busca casa, ignore-case, em
     * nome OU codigo OU apresentacao. RF-DAD-06.
     */
    @Query("""
            SELECT m FROM Medicamento m
            WHERE (:familia IS NULL OR m.familia = :familia)
              AND (:criticidade IS NULL OR m.criticidade = :criticidade)
              AND (:essencial IS NULL OR m.essencial = :essencial)
              AND (:ativo IS NULL OR m.ativo = :ativo)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(m.apresentacao) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<Medicamento> buscarComFiltros(@Param("familia") FamiliaTerapeutica familia,
                                       @Param("criticidade") Criticidade criticidade,
                                       @Param("essencial") Boolean essencial,
                                       @Param("ativo") Boolean ativo,
                                       @Param("busca") String busca,
                                       Sort sort);
}
