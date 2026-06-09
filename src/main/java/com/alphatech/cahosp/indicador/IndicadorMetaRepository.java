package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndicadorMetaRepository extends JpaRepository<IndicadorMeta, UUID> {

    /** Todos os indicadores com o historico carregado (lista do painel), na ordem de exibicao. */
    @Query("SELECT DISTINCT i FROM IndicadorMeta i LEFT JOIN FETCH i.historico ORDER BY i.ordem")
    List<IndicadorMeta> buscarTodosComHistorico();

    /** Um indicador (por codigo de negocio) com o historico carregado. */
    @Query("SELECT i FROM IndicadorMeta i LEFT JOIN FETCH i.historico WHERE i.codigo = :codigo")
    Optional<IndicadorMeta> findByCodigoComHistorico(@Param("codigo") String codigo);

    boolean existsByCodigo(String codigo);
}
