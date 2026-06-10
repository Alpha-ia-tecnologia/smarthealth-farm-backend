-- RF-SEG — trilha de auditoria e conformidade LGPD: quem fez o que, sobre qual recurso,
-- sob qual base legal e se a acao foi assistida por IA. Ledger imutavel (sem update/delete).

create table log_auditoria (
    id               uuid         primary key,
    data             timestamptz  not null,
    usuario_id       uuid,
    usuario_nome     varchar(120) not null,
    perfil           varchar(20),
    categoria        varchar(40)  not null,
    acao             varchar(200) not null,
    recurso          varchar(160) not null,
    base_legal       varchar(160),
    assistido_por_ia boolean      not null,
    ip               varchar(45),
    constraint ck_log_auditoria_perfil check (perfil is null or perfil in ('OPERADOR', 'GESTOR', 'TI')),
    constraint ck_log_auditoria_categoria check (categoria in (
        'APROVAR_RECOMENDACAO', 'EXECUTAR_RECOMENDACAO', 'RECALIBRAR_PREVISAO', 'GERAR_ALERTAS',
        'ALTERAR_LIMIAR_ALERTA', 'EXPORTAR_RELATORIO', 'INFERENCIA_IA', 'GERIR_USUARIO',
        'CONSULTAR', 'AUTENTICAR'))
);

-- A trilha e consultada por recencia e filtrada por categoria/perfil na revisao de conformidade.
create index ix_log_auditoria_data on log_auditoria (data desc);
create index ix_log_auditoria_categoria on log_auditoria (categoria);
