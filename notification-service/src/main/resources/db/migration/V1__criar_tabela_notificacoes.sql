CREATE TABLE notificacoes (
    id           UUID PRIMARY KEY,
    evento_id    UUID         NOT NULL,
    consulta_id  UUID         NOT NULL,
    paciente_id  UUID         NOT NULL,
    canal        VARCHAR(20)  NOT NULL,
    mensagem     VARCHAR(500) NOT NULL,
    enviada_em   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_notificacoes_evento_id UNIQUE (evento_id)
);

CREATE INDEX ix_notificacoes_paciente_id ON notificacoes (paciente_id);
