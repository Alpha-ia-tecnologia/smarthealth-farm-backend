package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.LogAuditoria;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Popula a trilha de auditoria com eventos historicos de demonstracao no startup, espelhando
 * {@code logsAuditoria}/{@code acoesAudit} do front (src/data/index.ts). RF-SEG-02.
 *
 * <p>Roda por ultimo ({@code @Order(95)}). <strong>Idempotente:</strong> so semeia se a trilha
 * estiver vazia — auditoria nao tem chave de negocio (cada evento e unico), entao a ausencia de
 * registros e a condicao de carga inicial. Os eventos reais (aprovar recomendacao, recalibrar
 * previsao, inferencia por IA...) sao acrescentados em tempo de execucao pelo
 * {@link RegistradorAuditoria} e nunca sao duplicados por este seeder.
 */
@Component
@Order(95)
public class AuditoriaSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSeeder.class);

    private record DefAcao(CategoriaAuditoria categoria, String acao, String recurso, String baseLegal,
                           boolean ia) {
    }

    private record DefUsuario(String nome, Perfil perfil) {
    }

    /** As 7 acoes auditadas do front (acoesAudit), com categoria tipada no servidor. */
    private static final List<DefAcao> ACOES = List.of(
            new DefAcao(CategoriaAuditoria.APROVAR_RECOMENDACAO,
                    "Aprovou recomendação de redistribuição", "RC-0003",
                    "Execução de contrato (art. 7º, V)", true),
            new DefAcao(CategoriaAuditoria.EXPORTAR_RELATORIO,
                    "Exportou relatório estratégico (PDF)", "Relatório CAHOSP Q2",
                    "Legítimo interesse (art. 7º, IX)", false),
            new DefAcao(CategoriaAuditoria.RECALIBRAR_PREVISAO,
                    "Recalibrou modelo de previsão", "Previsão m-002@HRI",
                    "Execução de contrato", true),
            new DefAcao(CategoriaAuditoria.ALTERAR_LIMIAR_ALERTA,
                    "Alterou limiar de alerta de desabastecimento", "Parâmetro ALE-desabastecimento",
                    "Execução de contrato", false),
            new DefAcao(CategoriaAuditoria.INFERENCIA_IA,
                    "Inferência via AI Gateway (dados anonimizados)", "AI Gateway · DeepSeek",
                    "Anonimização (art. 12)", true),
            new DefAcao(CategoriaAuditoria.GERIR_USUARIO,
                    "Cadastrou novo usuário", "usuario:operador",
                    "Execução de contrato", false),
            new DefAcao(CategoriaAuditoria.CONSULTAR,
                    "Consultou histórico de lote", "L-00231",
                    "Controle sanitário", false));

    /** Atores de demonstracao, cada um com perfil fixo (coerente, ao contrario do mock do front). */
    private static final List<DefUsuario> USUARIOS = List.of(
            new DefUsuario("Ana Sousa", Perfil.GESTOR),
            new DefUsuario("Marcos Lima", Perfil.OPERADOR),
            new DefUsuario("Rita Costa", Perfil.OPERADOR),
            new DefUsuario("João Pereira", Perfil.TI),
            new DefUsuario("Carla Mendes", Perfil.GESTOR));

    private static final int TOTAL_EVENTOS = 21;

    private final LogAuditoriaRepository repository;

    public AuditoriaSeeder(LogAuditoriaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<LogAuditoria> eventos = new ArrayList<>(TOTAL_EVENTOS);
        for (int i = 0; i < TOTAL_EVENTOS; i++) {
            DefAcao acao = ACOES.get(i % ACOES.size());
            DefUsuario usuario = USUARIOS.get(i % USUARIOS.size());
            eventos.add(new LogAuditoria(
                    instante(i),
                    null, // demo/historico: sem vinculo a um cadastro; eventos reais carregam usuarioId
                    usuario.nome(),
                    usuario.perfil(),
                    acao.categoria(),
                    acao.acao(),
                    acao.recurso(),
                    acao.baseLegal(),
                    acao.ia(),
                    ip(i)));
        }
        repository.saveAll(eventos);
        log.info("Auditoria semeada: {} eventos de demonstracao.", eventos.size());
    }

    /** Instante deterministico em 01–07/06/2026 (espelha a janela do mock do front). */
    private static Instant instante(int i) {
        int dia = 1 + (i % 7);
        int hora = 8 + (i % 10);
        int minuto = (i * 7) % 60;
        return Instant.parse(String.format("2026-06-%02dT%02d:%02d:00Z", dia, hora, minuto));
    }

    /** IP de origem deterministico (faixa interna 10.x.x.x). */
    private static String ip(int i) {
        return "10." + (i * 13 % 200) + "." + (i * 37 % 200) + "." + (i * 7 % 200);
    }
}
