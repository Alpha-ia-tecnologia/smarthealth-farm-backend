package com.alphatech.cahosp.alerta.dominio;

import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Alerta operacional gerado por regra (RF-ALE). Dois tipos:
 * <ul>
 *   <li>{@link TipoAlerta#DESABASTECIMENTO} — derivado do estoque/cobertura (medicamento essencial
 *       com saldo abaixo do nivel critico). RF-ALE-01.</li>
 *   <li>{@link TipoAlerta#VENCIMENTO} — derivado de um {@link Lote} proximo da validade. RF-ALE-02.</li>
 * </ul>
 *
 * <p>O alerta e um dado <strong>derivado</strong> (gerado pelo motor {@code GeradorAlerta} a partir
 * do estoque, da previsao e dos lotes), nao um dado lancado a mao. {@link #medicamento} e
 * {@link #unidade} sao referenciados por FK; {@link #lote} so existe no tipo vencimento. Os
 * {@link #destinatarios} (RF-ALE-04) sao um conjunto de {@link Perfil}.
 */
@Entity
@Table(name = "alerta")
@EntityListeners(AuditingEntityListener.class)
public class Alerta {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severidade severidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    /** So presente em alertas de vencimento (o lote em risco). RF-ALE-02. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAlerta status;

    /** Perfis direcionados a tratar o alerta (RF-ALE-04). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alerta_destinatario",
            joinColumns = @JoinColumn(name = "alerta_id"))
    @Column(name = "perfil", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Set<Perfil> destinatarios = EnumSet.noneOf(Perfil.class);

    /** Dias ate o evento (cobertura restante ou dias para vencer). */
    @Column(name = "dias_para_evento", nullable = false)
    private int diasParaEvento;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Alerta() {
        // JPA
    }

    public Alerta(TipoAlerta tipo, Severidade severidade, Medicamento medicamento, Unidade unidade,
                  Lote lote, String mensagem, Set<Perfil> destinatarios, int diasParaEvento) {
        this.tipo = tipo;
        this.severidade = severidade;
        this.medicamento = medicamento;
        this.unidade = unidade;
        this.lote = lote;
        this.mensagem = mensagem;
        this.destinatarios = destinatarios == null
                ? EnumSet.noneOf(Perfil.class) : EnumSet.copyOf(destinatarios);
        this.diasParaEvento = diasParaEvento;
        this.status = StatusAlerta.ABERTO;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /**
     * Aplica uma transicao de status (RF-ALE-05), validando a sequencia de tratamento.
     * {@code RESOLVIDO} e terminal; idempotente para o mesmo status atual.
     */
    public void mudarStatusPara(StatusAlerta novo) {
        if (novo == null) {
            throw new RegraNegocioException("Status do alerta e obrigatorio.");
        }
        if (novo == this.status) {
            return;
        }
        if (this.status == StatusAlerta.RESOLVIDO) {
            throw new RegraNegocioException(
                    "Alerta resolvido nao pode mudar de status; um novo alerta sera gerado se a condicao persistir.");
        }
        this.status = novo;
    }

    public UUID getId() {
        return id;
    }

    public TipoAlerta getTipo() {
        return tipo;
    }

    public Severidade getSeveridade() {
        return severidade;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public Lote getLote() {
        return lote;
    }

    public String getMensagem() {
        return mensagem;
    }

    public StatusAlerta getStatus() {
        return status;
    }

    public Set<Perfil> getDestinatarios() {
        return destinatarios;
    }

    public int getDiasParaEvento() {
        return diasParaEvento;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
