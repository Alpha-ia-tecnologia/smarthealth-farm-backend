-- RF-ADM / RF-SEG: novo perfil ADMIN (superusuario, acesso total via hierarquia de papeis).
-- O conjunto de perfis e fechado por CHECK em tres tabelas; recriamos cada um incluindo 'ADMIN'.

alter table usuario drop constraint ck_usuario_perfil;
alter table usuario add constraint ck_usuario_perfil
    check (perfil in ('OPERADOR', 'GESTOR', 'TI', 'ADMIN'));

alter table alerta_destinatario drop constraint ck_alerta_destinatario_perfil;
alter table alerta_destinatario add constraint ck_alerta_destinatario_perfil
    check (perfil in ('OPERADOR', 'GESTOR', 'TI', 'ADMIN'));

alter table log_auditoria drop constraint ck_log_auditoria_perfil;
alter table log_auditoria add constraint ck_log_auditoria_perfil
    check (perfil is null or perfil in ('OPERADOR', 'GESTOR', 'TI', 'ADMIN'));
