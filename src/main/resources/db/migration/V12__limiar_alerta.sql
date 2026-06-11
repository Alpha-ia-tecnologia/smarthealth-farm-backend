-- RF-ALE-03 — parametros/limiares que disparam os alertas (configuracao unica do sistema).
-- Linha singleton criada aqui com os defaults historicos do motor (espelham o front mockado).

create table limiar_alerta (
    id                           uuid        primary key,
    percentual_estoque_minimo    integer     not null,
    cobertura_critica_dias       integer     not null,
    cobertura_alta_dias          integer     not null,
    antecedencia_vencimento_dias integer     not null,
    vencimento_critico_dias      integer     not null,
    vencimento_alto_dias         integer     not null,
    desabastecimento_ativo       boolean     not null,
    vencimento_ativo             boolean     not null,
    atualizado_em                timestamptz not null,
    constraint ck_limiar_pct check (percentual_estoque_minimo between 10 and 200),
    constraint ck_limiar_cobertura
        check (cobertura_critica_dias > 0 and cobertura_alta_dias > cobertura_critica_dias),
    constraint ck_limiar_vencimento
        check (vencimento_critico_dias > 0
           and vencimento_alto_dias > vencimento_critico_dias
           and antecedencia_vencimento_dias >= vencimento_alto_dias)
);

insert into limiar_alerta (
    id, percentual_estoque_minimo, cobertura_critica_dias, cobertura_alta_dias,
    antecedencia_vencimento_dias, vencimento_critico_dias, vencimento_alto_dias,
    desabastecimento_ativo, vencimento_ativo, atualizado_em
) values (
    '00000000-0000-0000-0000-000000000001', 100, 5, 10, 60, 20, 40, true, true, now()
);
