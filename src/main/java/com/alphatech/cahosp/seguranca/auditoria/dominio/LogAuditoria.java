package com.alphatech.cahosp.seguranca.auditoria.dominio;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutavel da trilha de auditoria (RF-SEG-01/02/03): quem fez o que, sobre qual recurso,
 * sob qual base legal (LGPD) e se a acao foi assistida por IA.
 *
 * <p><strong>Ledger imutavel</strong> (como a {@code Movimentacao} do estoque): nunca e alterado
 * apos criado. Por isso o ator e gravado como <em>snapshot</em> ({@link #usuarioNome}/
 * {@link #perfil}) mais uma referencia fraca {@link #usuarioId} (UUID simples, sem {@code @ManyToOne}):
 * o log preserva o registro historico mesmo que o usuario seja removido, e nao quebra a integridade
 * do ledger ao depender do ciclo de vida do cadastro. {@link #usuarioId} fica nulo em acoes do
 * proprio sistema.
 */
@Entity
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** Momento do evento auditado (RF-SEG-02). Para logs de runtime, igual ao instante de gravacao. */
    @Column(nullable = false)
    private Instant data;

    /** Referencia fraca ao usuario ator (nulo em acoes do sistema). Ver doc da classe. */
    @Column(name = "usuario_id", columnDefinition = "uuid")
    private UUID usuarioId;

    @Column(name = "usuario_nome", nullable = false, length = 120)
    private String usuarioNome;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaAuditoria categoria;

    @Column(nullable = false, length = 200)
    private String acao;

    @Column(nullable = false, length = 160)
    private String recurso;

    @Column(name = "base_legal", length = 160)
    private String baseLegal;

    @Column(name = "assistido_por_ia", nullable = false)
    private boolean assistidoPorIa;

    @Column(length = 45)
    private String ip;

    protected LogAuditoria() {
    }

    public LogAuditoria(Instant data, UUID usuarioId, String usuarioNome, Perfil perfil,
                        CategoriaAuditoria categoria, String acao, String recurso,
                        String baseLegal, boolean assistidoPorIa, String ip) {
        this.data = data;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.perfil = perfil;
        this.categoria = categoria;
        this.acao = acao;
        this.recurso = recurso;
        this.baseLegal = baseLegal;
        this.assistidoPorIa = assistidoPorIa;
        this.ip = ip;
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

    public Instant getData() {
        return data;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public CategoriaAuditoria getCategoria() {
        return categoria;
    }

    public String getAcao() {
        return acao;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getBaseLegal() {
        return baseLegal;
    }

    public boolean isAssistidoPorIa() {
        return assistidoPorIa;
    }

    public String getIp() {
        return ip;
    }
}
