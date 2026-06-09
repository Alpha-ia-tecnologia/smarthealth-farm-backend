package com.alphatech.cahosp.previsao.dominio;

import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Previsao de demanda de um medicamento em uma unidade (RF-PRV). Guarda a assertividade
 * ({@link #mape}), o modelo/versao, o {@link #drift} e a {@link #serie} temporal (realizado x
 * previsto). RF-EST-04: o nivel critico do estoque se apoia nesta previsao.
 */
@Entity
@Table(name = "previsao",
        uniqueConstraints = @UniqueConstraint(name = "uk_previsao_med_unidade",
                columnNames = {"medicamento_id", "unidade_id"}))
@EntityListeners(AuditingEntityListener.class)
public class Previsao {

    /** Piso de MAPE ao recalibrar (nao reduz indefinidamente). */
    private static final BigDecimal MAPE_MINIMO = new BigDecimal("3.00");

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(name = "horizonte_meses", nullable = false)
    private int horizonteMeses;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal mape;

    @Column(nullable = false, length = 80)
    private String modelo;

    @Column(name = "versao_modelo", nullable = false, length = 20)
    private String versaoModelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Drift drift;

    @Column(name = "calibrado_em", nullable = false)
    private LocalDate calibradoEm;

    @OneToMany(mappedBy = "previsao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<PontoSerie> serie = new ArrayList<>();

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Previsao() {
        // JPA
    }

    public Previsao(Medicamento medicamento, Unidade unidade, int horizonteMeses, BigDecimal mape,
                    String modelo, String versaoModelo, Drift drift, LocalDate calibradoEm) {
        this.medicamento = medicamento;
        this.unidade = unidade;
        this.horizonteMeses = horizonteMeses;
        this.mape = mape;
        this.modelo = modelo;
        this.versaoModelo = versaoModelo;
        this.drift = drift;
        this.calibradoEm = calibradoEm;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /** Adiciona um ponto a serie, mantendo a referencia inversa. */
    public void adicionarPonto(PontoSerie ponto) {
        ponto.vincular(this);
        this.serie.add(ponto);
    }

    /**
     * Recalibra o modelo (RF-PRV — acao de Gestor): estabiliza o drift, reduz o erro em 10%
     * (ate o piso) e marca a data de calibracao.
     */
    public void recalibrar(LocalDate quando) {
        this.drift = Drift.ESTAVEL;
        BigDecimal reduzido = this.mape.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
        this.mape = reduzido.max(MAPE_MINIMO);
        this.calibradoEm = quando;
    }

    public UUID getId() {
        return id;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public int getHorizonteMeses() {
        return horizonteMeses;
    }

    public BigDecimal getMape() {
        return mape;
    }

    public String getModelo() {
        return modelo;
    }

    public String getVersaoModelo() {
        return versaoModelo;
    }

    public Drift getDrift() {
        return drift;
    }

    public LocalDate getCalibradoEm() {
        return calibradoEm;
    }

    public List<PontoSerie> getSerie() {
        return serie;
    }
}
