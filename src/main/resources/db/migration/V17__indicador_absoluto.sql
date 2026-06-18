-- RF-IND — valor absoluto que dá lastro às taxas (%): ex. "9 de 80 itens essenciais" por trás da
-- taxa de desabastecimento de 11,2%. Opcional: nulo onde a unidade já é absoluta (R$ mil / dias /
-- un) ou onde não há contagem (MAPE). Colunas aditivas + backfill dos indicadores demo já semeados
-- (em bancos novos, o IndicadorSeeder grava os mesmos valores na inserção).

alter table indicador add column numerador_absoluto   numeric(12,2);
alter table indicador add column denominador_absoluto numeric(12,2);
alter table indicador add column unidade_absoluta      varchar(30);

update indicador
   set numerador_absoluto = 9, denominador_absoluto = 80, unidade_absoluta = 'itens essenciais'
 where codigo = 'ind-ruptura';

update indicador
   set numerador_absoluto = 13, denominador_absoluto = 302, unidade_absoluta = 'lotes'
 where codigo = 'ind-vencimento';
