-- RF-ADM-01 — vinculo opcional do usuario com a sua unidade de lotacao (FK).
-- Aditivo e nao-quebravel: coluna anulavel (usuarios existentes ficam sem unidade).
alter table usuario
    add column unidade_id uuid;

alter table usuario
    add constraint fk_usuario_unidade foreign key (unidade_id) references unidade (id);

create index ix_usuario_unidade on usuario (unidade_id);
