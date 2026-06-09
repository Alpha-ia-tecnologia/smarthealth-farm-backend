package com.alphatech.cahosp.ingestao.dominio;

import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Indicadores de maturidade e qualidade da base historica por familia terapeutica (RF-DAD-04):
 * completude, consistencia, granularidade e lacunas sinalizadas.
 */
@Entity
@Table(name = "qualidade_familia")
@EntityListeners(AuditingEntityListener.class)
public class QualidadeFamilia {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, unique = true)
    private FamiliaTerapeutica familia;

    @Column(nullable = false)
    private int maturidade;

    @Column(nullable = false)
    private int completude;

    @Column(nullable = false)
    private int consistencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GranularidadeDado granularidade;

    @Column(nullable = false)
    private int lacunas;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected QualidadeFamilia() {
    }

    public QualidadeFamilia(FamiliaTerapeutica familia, int maturidade, int completude, int consistencia,
                           GranularidadeDado granularidade, int lacunas) {
        this.familia = familia;
        this.maturidade = maturidade;
        this.completude = completude;
        this.consistencia = consistencia;
        this.granularidade = granularidade;
        this.lacunas = lacunas;
    }

    @PrePersist
    void gerarId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public FamiliaTerapeutica getFamilia() {
        return familia;
    }

    public int getMaturidade() {
        return maturidade;
    }

    public int getCompletude() {
        return completude;
    }

    public int getConsistencia() {
        return consistencia;
    }

    public GranularidadeDado getGranularidade() {
        return granularidade;
    }

    public int getLacunas() {
        return lacunas;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
