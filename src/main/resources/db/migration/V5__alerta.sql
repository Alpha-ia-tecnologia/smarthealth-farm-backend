-- RF-ALE — alertas operacionais gerados por regra (desabastecimento + vencimento).
-- Derivados do estoque/cobertura (RF-ALE-01) e da validade dos lotes (RF-ALE-02).

create table alerta (
    id               uuid         primary key,
    tipo             varchar(20)  not null,
    severidade       varchar(10)  not null,
    insumo_id   uuid         not null references insumo (id),
    unidade_id       uuid         not null references unidade (id),
    lote_id          uuid         references lote (id),
    mensagem         varchar(300) not null,
    status           varchar(20)  not null,
    dias_para_evento integer      not null,
    criado_em        timestamptz  not null,
    atualizado_em    timestamptz  not null,
    constraint ck_alerta_tipo check (tipo in ('DESABASTECIMENTO', 'VENCIMENTO')),
    constraint ck_alerta_severidade check (severidade in ('CRITICO', 'ALTO', 'MEDIO')),
    constraint ck_alerta_status check (status in ('ABERTO', 'EM_TRATAMENTO', 'RESOLVIDO'))
);

create index ix_alerta_status on alerta (status);
create index ix_alerta_tipo on alerta (tipo);
create index ix_alerta_unidade on alerta (unidade_id);
create index ix_alerta_lote on alerta (lote_id);

-- RF-ALE-04 — perfis direcionados a tratar cada alerta (@ElementCollection de Perfil).
create table alerta_destinatario (
    alerta_id uuid        not null references alerta (id),
    perfil    varchar(20) not null,
    constraint pk_alerta_destinatario primary key (alerta_id, perfil),
    constraint ck_alerta_destinatario_perfil check (perfil in ('OPERADOR', 'GESTOR', 'TI'))
);
