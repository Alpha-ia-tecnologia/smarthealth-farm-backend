-- RF-DAD-06 — catalogo base: unidades hospitalares e insumos/insumos.
-- Referenciados por FK em estoque, previsao, alerta e recomendacao nas fases seguintes.

create table unidade (
    id                 uuid         primary key,
    nome               varchar(150) not null,
    sigla              varchar(20)  not null,
    municipio          varchar(120) not null,
    porte              varchar(20)  not null,
    leitos             integer      not null,
    conectividade      varchar(20)  not null,
    perfil_demografico varchar(200) not null,
    hub                boolean      not null default false,
    ativo              boolean      not null default true,
    criado_em          timestamptz  not null,
    atualizado_em      timestamptz  not null,
    constraint uk_unidade_sigla unique (sigla),
    constraint ck_unidade_porte check (porte in ('PEQUENO', 'MEDIO', 'GRANDE')),
    constraint ck_unidade_conectividade check (conectividade in ('ESTAVEL', 'INTERMITENTE', 'PRECARIA')),
    constraint ck_unidade_leitos_nao_negativo check (leitos >= 0)
);

create index ix_unidade_hub on unidade (hub);
create index ix_unidade_ativo on unidade (ativo);

create table insumo (
    id             uuid         primary key,
    codigo         varchar(30)  not null,
    nome           varchar(200) not null,
    apresentacao   varchar(80)  not null,
    categoria        varchar(40)  not null,
    unidade_medida varchar(20)  not null,
    criticidade    varchar(10)  not null,
    essencial      boolean      not null,
    ativo          boolean      not null default true,
    criado_em      timestamptz  not null,
    atualizado_em  timestamptz  not null,
    constraint uk_insumo_codigo unique (codigo),
    constraint ck_insumo_categoria check (categoria in (
        'ANTIBIOTICOS', 'ANALGESICOS', 'ANTIVIRAIS', 'CARDIOVASCULAR',
        'SOROS_E_VACINAS', 'INSUMOS_MEDICOS', 'SAUDE_MENTAL', 'ANTIPARASITARIOS'
    )),
    constraint ck_insumo_criticidade check (criticidade in ('ALTA', 'MEDIA', 'BAIXA'))
);

create index ix_insumo_categoria on insumo (categoria);
create index ix_insumo_criticidade on insumo (criticidade);
create index ix_insumo_ativo on insumo (ativo);
