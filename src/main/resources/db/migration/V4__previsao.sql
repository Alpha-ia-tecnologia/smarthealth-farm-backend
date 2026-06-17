-- RF-PRV — previsao de demanda por (insumo, unidade) e sua serie temporal.
-- RF-EST-04: o nivel critico do estoque se apoia nestas previsoes.

create table previsao (
    id              uuid         primary key,
    insumo_id  uuid         not null references insumo (id),
    unidade_id      uuid         not null references unidade (id),
    horizonte_meses integer      not null,
    mape            numeric(5,2) not null,
    modelo          varchar(80)  not null,
    versao_modelo   varchar(20)  not null,
    drift           varchar(20)  not null,
    calibrado_em    date         not null,
    criado_em       timestamptz  not null,
    atualizado_em   timestamptz  not null,
    constraint uk_previsao_insumo_unidade unique (insumo_id, unidade_id),
    constraint ck_previsao_drift check (drift in ('ESTAVEL', 'ATENCAO', 'DEGRADADO'))
);

create index ix_previsao_unidade on previsao (unidade_id);
create index ix_previsao_drift on previsao (drift);

create table ponto_serie (
    id              uuid        primary key,
    previsao_id     uuid        not null references previsao (id),
    periodo         varchar(7)  not null,
    ordem           integer     not null,
    realizado       integer,
    previsto        integer,
    limite_inferior integer,
    limite_superior integer
);

create index ix_ponto_serie_previsao on ponto_serie (previsao_id);
