-- RF-ALE-05 / RF-SEG-02: o tratamento de alerta (Aberto -> Em tratamento -> Resolvido) passa a
-- ser auditado, registrando quem fez a acao. Adiciona a categoria 'TRATAR_ALERTA' ao conjunto
-- permitido na trilha. O CHECK e recriado porque o conjunto de categorias e fechado (ver V11).

alter table log_auditoria drop constraint ck_log_auditoria_categoria;

alter table log_auditoria add constraint ck_log_auditoria_categoria check (categoria in (
    'APROVAR_RECOMENDACAO', 'EXECUTAR_RECOMENDACAO', 'RECALIBRAR_PREVISAO', 'GERAR_ALERTAS',
    'TRATAR_ALERTA', 'ALTERAR_LIMIAR_ALERTA', 'EXPORTAR_RELATORIO', 'INFERENCIA_IA',
    'GERIR_USUARIO', 'CONSULTAR', 'AUTENTICAR'));
