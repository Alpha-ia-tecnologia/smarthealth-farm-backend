package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.LimiarAlerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Persistencia da configuracao de limiares (linha singleton — ver {@link LimiarAlerta#ID_CONFIG}). */
public interface LimiarAlertaRepository extends JpaRepository<LimiarAlerta, UUID> {
}
