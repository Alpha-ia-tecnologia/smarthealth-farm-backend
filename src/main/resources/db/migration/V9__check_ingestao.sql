-- RF-DAD — alinha a V8 ao padrao das demais migrations: CHECK constraints garantindo a
-- integridade dos enums persistidos como texto (status da fonte e granularidade).

alter table fonte_dado
    add constraint ck_fonte_dado_status check (status in ('SINCRONIZADO', 'ATRASADO', 'ERRO'));

alter table qualidade_categoria
    add constraint ck_qualidade_categoria_granularidade
        check (granularidade in ('DIARIA', 'SEMANAL', 'MENSAL'));
