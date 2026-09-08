CREATE TABLE usuarios (
    id             UUID PRIMARY KEY,
    nome_completo  VARCHAR(150) NOT NULL,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    criado_em      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_role CHECK (role IN ('MEDICO', 'ENFERMEIRO', 'PACIENTE'))
);
