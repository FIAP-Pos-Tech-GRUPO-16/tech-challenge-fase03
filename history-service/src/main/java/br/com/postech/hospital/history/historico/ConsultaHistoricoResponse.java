package br.com.postech.hospital.history.historico;

import java.util.UUID;

/** Formato exposto pelo schema GraphQL — datas como texto ISO-8601, sem depender de um scalar customizado. */
public record ConsultaHistoricoResponse(
        UUID id,
        UUID pacienteId,
        UUID medicoId,
        String dataHora,
        String status,
        String atualizadoEm
) {

    public static ConsultaHistoricoResponse de(ConsultaHistorico historico) {
        return new ConsultaHistoricoResponse(
                historico.getConsultaId(),
                historico.getPacienteId(),
                historico.getMedicoId(),
                historico.getDataHora().toString(),
                historico.getStatus().name(),
                historico.getUltimaAtualizacaoEvento().toString()
        );
    }
}
