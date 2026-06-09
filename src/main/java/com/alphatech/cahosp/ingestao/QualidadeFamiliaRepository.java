package com.alphatech.cahosp.ingestao;

import com.alphatech.cahosp.ingestao.dominio.QualidadeFamilia;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QualidadeFamiliaRepository extends JpaRepository<QualidadeFamilia, UUID> {

    Optional<QualidadeFamilia> findByFamilia(FamiliaTerapeutica familia);

    boolean existsByFamilia(FamiliaTerapeutica familia);

    List<QualidadeFamilia> findAllByOrderByFamiliaAsc();
}
