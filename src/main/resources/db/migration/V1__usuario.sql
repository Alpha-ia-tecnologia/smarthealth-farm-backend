-- RF-ADM / RF-SEG — usuarios do sistema (login por e-mail, senha BCrypt, RBAC por perfil).
create table usuario (
    id            uuid         primary key,
    nome          varchar(150) not null,
    email         varchar(180) not null,
    senha_hash    varchar(100) not null,
    perfil        varchar(20)  not null,
    ativo         boolean      not null default true,
    ultimo_acesso timestamptz,
    criado_em     timestamptz  not null,
    atualizado_em timestamptz  not null,
    constraint uk_usuario_email unique (email),
    constraint ck_usuario_perfil check (perfil in ('OPERADOR', 'GESTOR', 'TI'))
);
