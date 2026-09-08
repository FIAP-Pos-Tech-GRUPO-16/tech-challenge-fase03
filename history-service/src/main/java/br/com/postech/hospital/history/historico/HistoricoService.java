package br.com.postech.hospital.history.historico;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mantém o modelo de leitura do histórico a partir dos eventos de consulta e resolve as
 * consultas GraphQL respeitando a mesma regra de posse do agendamento: um paciente só enxerga
 * o próprio histórico.
 */
@Service
public class HistoricoService {

    private static final Logger log = LoggerFactory.getLogger(HistoricoService.class);

    private final ConsultaHistoricoRepository repository;

    public HistoricoService(ConsultaHistoricoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void aplicar(ConsultaEvent evento) {
        repository.findById(evento.consultaId()).ifPresentOrElse(
                existente -> atualizarSeMaisRecente(existente, evento),
                () -> criar(evento)
        );
    }

    private void criar(ConsultaEvent evento) {
        ConsultaHistorico historico = new ConsultaHistorico(evento.consultaId(), evento.pacienteId(),
                evento.medicoId(), evento.dataHora(), evento.status(), evento.ocorridoEm());
        repository.save(historico);
        log.info("Histórico criado a partir do evento: consultaId={} eventoId={}", evento.consultaId(), evento.eventoId());
    }

    private void atualizarSeMaisRecente(ConsultaHistorico existente, ConsultaEvent evento) {
        if (!existente.maisRecenteQue(evento.ocorridoEm())) {
            log.info("Evento desatualizado ou duplicado ignorado: consultaId={} eventoId={}", evento.consultaId(), evento.eventoId());
            return;
        }
        existente.aplicarEvento(evento.pacienteId(), evento.medicoId(), evento.dataHora(), evento.status(), evento.ocorridoEm());
        repository.save(existente);
        log.info("Histórico atualizado a partir do evento: consultaId={} eventoId={}", evento.consultaId(), evento.eventoId());
    }

    @Transactional(readOnly = true)
    public List<ConsultaHistoricoResponse> consultasDoPaciente(UUID pacienteId, AuthenticatedUser autor) {
        garantirAcesso(pacienteId, autor);
        return repository.findByPacienteId(pacienteId).stream().map(ConsultaHistoricoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultaHistoricoResponse> consultasFuturasDoPaciente(UUID pacienteId, AuthenticatedUser autor) {
        garantirAcesso(pacienteId, autor);
        return repository.findByPacienteIdAndDataHoraAfter(pacienteId, LocalDateTime.now())
                .stream().map(ConsultaHistoricoResponse::de).toList();
    }

    private void garantirAcesso(UUID pacienteId, AuthenticatedUser autor) {
        if (autor.isPaciente() && !autor.id().equals(pacienteId)) {
            throw new AccessDeniedException("Paciente só pode consultar o próprio histórico");
        }
    }
}
