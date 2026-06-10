package com.alphatech.cahosp.integracao;

import com.alphatech.cahosp.integracao.dominio.IntegracaoApi;
import com.alphatech.cahosp.integracao.dominio.StatusIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface IntegracaoApiRepository extends JpaRepository<IntegracaoApi, UUID> {

    List<IntegracaoApi> findAllByOrderByOrdemAsc();

    boolean existsByCodigoIgnoreCase(String codigo);

    long countByStatus(StatusIntegracao status);

    /** Total de registros acumulados em buffer offline (RF-INT-05). */
    @Query("SELECT COALESCE(SUM(i.registrosBuffer), 0) FROM IntegracaoApi i")
    long somarRegistrosBuffer();
}
