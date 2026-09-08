CREATE TABLE consultas (
    id                 UUID PRIMARY KEY,
    paciente_id        UUID         NOT NULL REFERENCES usuarios (id),
    medico_id          UUID         NOT NULL REFERENCES usuarios (id),
    registrada_por_id  UUID         NOT NULL REFERENCES usuarios (id),
    data_hora          TIMESTAMP    NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    observacoes        VARCHAR(1000),
    criado_em          TIMESTAMP    NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT ck_consultas_status CHECK (status IN ('AGENDADA', 'REALIZADA', 'CANCELADA'))
);

CREATE INDEX ix_consultas_paciente_id ON consultas (paciente_id);
CREATE INDEX ix_consultas_medico_id ON consultas (medico_id);
