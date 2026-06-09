-- RF-DAD — fontes de dados ingeridas e qualidade da base historica por familia terapeutica.

create table fonte_dado (
    id               uuid          primary key,
    codigo           varchar(40)   not null unique,
    nome             varchar(150)  not null,
    geracao          varchar(80)   not null,
    status           varchar(20)   not null,
    ultima_ingestao  timestamptz   not null,
    registros        bigint        not null,
    qualidade        integer       not null,
    procedencia      varchar(200)  not null,
    ordem            integer       not null,
    criado_em        timestamptz   not null,
    atualizado_em    timestamptz   not null
);

create table qualidade_familia (
    id            uuid         primary key,
    familia       varchar(30)  not null unique,
    maturidade    integer      not null,
    completude    integer      not null,
    consistencia  integer      not null,
    granularidade varchar(20)  not null,
    lacunas       integer      not null,
    criado_em     timestamptz  not null,
    atualizado_em timestamptz  not null
);
