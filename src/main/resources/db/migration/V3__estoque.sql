-- RF-EST — estoque por lote, livro-razao de movimentacoes e posicao consolidada.
-- Lote e Movimentacao referenciam Medicamento e Unidade por FK (catalogo, V2).

create table lote (
    id             uuid         primary key,
    medicamento_id uuid         not null references medicamento (id),
    unidade_id     uuid         not null references unidade (id),
    numero_lote    varchar(40)  not null,
    validade       date         not null,
    quantidade     integer      not null,
    fabricante     varchar(80)  not null,
    criado_em      timestamptz  not null,
    atualizado_em  timestamptz  not null,
    constraint ck_lote_quantidade_nao_negativa check (quantidade >= 0)
);

create index ix_lote_medicamento on lote (medicamento_id);
create index ix_lote_unidade on lote (unidade_id);
create index ix_lote_validade on lote (validade);

-- Livro-razao append-only (imutavel): sem updates/deletes de regra de negocio.
create table movimentacao (
    id             uuid         primary key,
    lote_id        uuid         not null references lote (id),
    medicamento_id uuid         not null references medicamento (id),
    unidade_id     uuid         not null references unidade (id),
    tipo           varchar(20)  not null,
    quantidade     integer      not null,
    data_hora      timestamptz  not null,
    responsavel    varchar(120) not null,
    documento      varchar(60)  not null,
    criado_em      timestamptz  not null,
    constraint ck_movimentacao_tipo check (tipo in ('ENTRADA', 'SAIDA', 'TRANSFERENCIA', 'AJUSTE')),
    -- AJUSTE (recontagem) pode zerar o lote; tipos com fluxo (>0) sao validados no servico.
    constraint ck_movimentacao_quantidade_nao_negativa check (quantidade >= 0)
);

create index ix_movimentacao_lote on movimentacao (lote_id);
create index ix_movimentacao_medicamento_unidade on movimentacao (medicamento_id, unidade_id);
create index ix_movimentacao_data on movimentacao (data_hora);

-- Posicao consolidada por (medicamento, unidade). quantidade = projecao do livro-razao.
create table posicao_estoque (
    id                              uuid        primary key,
    medicamento_id                  uuid        not null references medicamento (id),
    unidade_id                      uuid        not null references unidade (id),
    quantidade                      integer     not null,
    nivel_critico                   integer     not null,
    estoque_maximo                  integer     not null,
    consumo_medio_diario            integer     not null,
    tempo_medio_ressuprimento_dias  integer     not null,
    criado_em                       timestamptz not null,
    atualizado_em                   timestamptz not null,
    constraint uk_posicao_med_unidade unique (medicamento_id, unidade_id),
    constraint ck_posicao_quantidade_nao_negativa check (quantidade >= 0)
);

create index ix_posicao_unidade on posicao_estoque (unidade_id);
