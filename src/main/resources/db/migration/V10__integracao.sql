-- RF-INT — integracoes com sistemas externos da EMSERH e provedores de IA do AI Gateway.

create table integracao_api (
    id               uuid         primary key,
    codigo           varchar(40)  not null unique,
    nome             varchar(120) not null,
    versao           varchar(20)  not null,
    status           varchar(20)  not null,
    latencia_ms      integer      not null,
    ultima_sync      timestamptz  not null,
    modo             varchar(30)  not null,
    registros_buffer integer      not null,
    ordem            integer      not null,
    criado_em        timestamptz  not null,
    atualizado_em    timestamptz  not null,
    constraint ck_integracao_status check (status in ('OPERACIONAL', 'DEGRADADA', 'INDISPONIVEL')),
    constraint ck_integracao_modo check (modo in ('ONLINE', 'OFFLINE_BUFFER', 'RECONCILIANDO'))
);

create table provedor_ia (
    id                  uuid          primary key,
    codigo              varchar(40)   not null unique,
    nome                varchar(80)   not null,
    ativo               boolean       not null,
    papel               varchar(20)   not null,
    custo_por_1k_tokens numeric(10,4) not null,
    chamadas_mes        bigint        not null,
    anonimizacao        boolean       not null,
    ordem               integer       not null,
    criado_em           timestamptz   not null,
    atualizado_em       timestamptz   not null,
    constraint ck_provedor_ia_papel check (papel in ('PRIMARIO', 'FALLBACK', 'STANDBY'))
);
