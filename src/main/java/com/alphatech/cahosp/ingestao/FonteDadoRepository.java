package com.alphatech.cahosp.ingestao;

import com.alphatech.cahosp.ingestao.dominio.FonteDado;
import com.alphatech.cahosp.ingestao.dominio.StatusFonte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FonteDadoRepository extends JpaRepository<FonteDado, UUID> {

    Optional<FonteDado> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    List<FonteDado> findAllByOrderByOrdemAsc();

    long countByStatus(StatusFonte status);
}
