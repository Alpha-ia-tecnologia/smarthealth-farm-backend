-- RF-REC — recomendacoes de reposicao e redistribuicao geradas por regra,
-- dimensionadas pela previsao/estoque (RF-REC-01/03), com economia estimada em R$ (RF-REC-02).

create table recomendacao (
    id                 uuid          primary key,
    tipo               varchar(20)   not null,
    insumo_id     uuid          not null references insumo (id),
    unidade_destino_id uuid          not null references unidade (id),
    unidade_origem_id  uuid          references unidade (id),
    quantidade         integer       not null,
    justificativa      varchar(400)  not null,
    origem_motor       varchar(30)   not null,
    prioridade         varchar(20)   not null,
    economia_estimada  numeric(12,2) not null,
    status             varchar(20)   not null,
    criado_em          timestamptz   not null,
    atualizado_em      timestamptz   not null,
    constraint ck_recomendacao_tipo check (tipo in ('REPOSICAO', 'REDISTRIBUICAO')),
    constraint ck_recomendacao_origem_motor check (origem_motor in ('REGRAS', 'APRENDIZADO_MAQUINA')),
    constraint ck_recomendacao_prioridade check (prioridade in ('ESSENCIAL', 'IMPORTANTE', 'DESEJAVEL')),
    constraint ck_recomendacao_status check (status in ('PENDENTE', 'APROVADA', 'EXECUTADA'))
);

create index ix_recomendacao_status on recomendacao (status);
create index ix_recomendacao_tipo on recomendacao (tipo);
create index ix_recomendacao_origem_motor on recomendacao (origem_motor);
create index ix_recomendacao_destino on recomendacao (unidade_destino_id);
create index ix_recomendacao_insumo on recomendacao (insumo_id);
