package br.com.postech.hospital.history.graphql;

import br.com.postech.hospital.history.historico.ConsultaHistoricoResponse;
import br.com.postech.hospital.history.historico.HistoricoService;
import br.com.postech.hospital.security.AuthenticatedUser;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class HistoricoGraphQlController {

    private final HistoricoService historicoService;

    public HistoricoGraphQlController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @QueryMapping
    public List<ConsultaHistoricoResponse> consultasPorPaciente(@Argument UUID pacienteId) {
        return historicoService.consultasDoPaciente(pacienteId, AuthenticatedUser.current());
    }

    @QueryMapping
    public List<ConsultaHistoricoResponse> consultasFuturasPorPaciente(@Argument UUID pacienteId) {
        return historicoService.consultasFuturasDoPaciente(pacienteId, AuthenticatedUser.current());
    }

    @QueryMapping
    public List<ConsultaHistoricoResponse> minhasConsultas() {
        AuthenticatedUser autor = exigirPaciente();
        return historicoService.consultasDoPaciente(autor.id(), autor);
    }

    @QueryMapping
    public List<ConsultaHistoricoResponse> minhasConsultasFuturas() {
        AuthenticatedUser autor = exigirPaciente();
        return historicoService.consultasFuturasDoPaciente(autor.id(), autor);
    }

    private AuthenticatedUser exigirPaciente() {
        AuthenticatedUser autor = AuthenticatedUser.current();
        if (!autor.isPaciente()) {
            throw new AccessDeniedException("Esta consulta é restrita a pacientes");
        }
        return autor;
    }
}
