-- RF-EST — custo unitario (R$) do insumo, base do valor de consumo na Curva ABC.
-- Coluna aditiva e nula: o InsumoSeeder preenche os custos do catalogo demo no startup (inclusive
-- backfill das linhas ja existentes), de forma idempotente.

alter table insumo add column custo_unitario numeric(12,2);
