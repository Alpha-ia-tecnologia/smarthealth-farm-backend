package com.alphatech.cahosp.ingestao;

import com.alphatech.cahosp.ingestao.dominio.QualidadeCategoria;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QualidadeCategoriaRepository extends JpaRepository<QualidadeCategoria, UUID> {

    Optional<QualidadeCategoria> findByCategoria(CategoriaInsumo categoria);

    boolean existsByCategoria(CategoriaInsumo categoria);

    List<QualidadeCategoria> findAllByOrderByCategoriaAsc();
}
