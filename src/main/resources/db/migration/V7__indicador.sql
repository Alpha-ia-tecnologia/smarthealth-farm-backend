-- RF-IND — indicadores de desempenho do projeto (meta x linha de base) e seu historico mensal.

create table indicador (
    id               uuid          primary key,
    codigo           varchar(40)   not null unique,
    nome             varchar(120)  not null,
    unidade_medida   varchar(20)   not null,
    baseline         numeric(12,2) not null,
    atual            numeric(12,2) not null,
    meta             numeric(12,2) not null,
    meta_reducao_pct integer       not null,
    melhor_menor     boolean       not null,
    ordem            integer       not null,
    criado_em        timestamptz   not null,
    atualizado_em    timestamptz   not null
);

create table ponto_historico (
    id           uuid          primary key,
    indicador_id uuid          not null references indicador (id),
    periodo      varchar(7)    not null,
    ordem        integer       not null,
    valor        numeric(12,2) not null
);

create index ix_ponto_historico_indicador on ponto_historico (indicador_id);
