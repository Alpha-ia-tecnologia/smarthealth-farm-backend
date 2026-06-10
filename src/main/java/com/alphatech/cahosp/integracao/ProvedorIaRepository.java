package com.alphatech.cahosp.integracao;

import com.alphatech.cahosp.integracao.dominio.ProvedorIa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProvedorIaRepository extends JpaRepository<ProvedorIa, UUID> {

    List<ProvedorIa> findAllByOrderByOrdemAsc();

    boolean existsByCodigoIgnoreCase(String codigo);
}
