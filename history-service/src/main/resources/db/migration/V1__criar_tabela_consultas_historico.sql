CREATE TABLE consultas_historico (
    consulta_id                UUID PRIMARY KEY,
    paciente_id                UUID        NOT NULL,
    medico_id                  UUID        NOT NULL,
    data_hora                  TIMESTAMP   NOT NULL,
    status                     VARCHAR(20) NOT NULL,
    ultima_atualizacao_evento  TIMESTAMP   NOT NULL
);

CREATE INDEX ix_consultas_historico_paciente_id ON consultas_historico (paciente_id);
