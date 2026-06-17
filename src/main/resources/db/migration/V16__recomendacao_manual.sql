-- RF-REC-05: recomendações manuais (criar/editar transferência) + recusa.
-- Novos valores fechados por CHECK: status 'RECUSADA', origem do motor 'MANUAL' e três categorias
-- de auditoria (criar/editar/recusar). Recriamos cada CHECK incluindo os novos valores.

alter table recomendacao drop constraint ck_recomendacao_status;
alter table recomendacao add constraint ck_recomendacao_status
    check (status in ('PENDENTE', 'APROVADA', 'EXECUTADA', 'RECUSADA'));

alter table recomendacao drop constraint ck_recomendacao_origem_motor;
alter table recomendacao add constraint ck_recomendacao_origem_motor
    check (origem_motor in ('REGRAS', 'APRENDIZADO_MAQUINA', 'MANUAL'));

alter table log_auditoria drop constraint ck_log_auditoria_categoria;
alter table log_auditoria add constraint ck_log_auditoria_categoria check (categoria in (
    'APROVAR_RECOMENDACAO', 'EXECUTAR_RECOMENDACAO', 'CRIAR_RECOMENDACAO', 'EDITAR_RECOMENDACAO',
    'RECUSAR_RECOMENDACAO', 'RECALIBRAR_PREVISAO', 'GERAR_ALERTAS', 'TRATAR_ALERTA',
    'ALTERAR_LIMIAR_ALERTA', 'EXPORTAR_RELATORIO', 'INFERENCIA_IA', 'GERIR_USUARIO',
    'CONSULTAR', 'AUTENTICAR'));
