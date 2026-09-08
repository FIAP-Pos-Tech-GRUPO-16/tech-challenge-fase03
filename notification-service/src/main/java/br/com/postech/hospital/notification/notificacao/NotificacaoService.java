package br.com.postech.hospital.notification.notificacao;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Processa eventos de consulta e simula o envio de um lembrete ao paciente (aqui, apenas
 * persiste o registro e escreve um log — não há integração real com e-mail/SMS, fora do
 * escopo do desafio).
 *
 * <p>{@code eventoId} é checado antes e também protegido por índice único no banco: a checagem
 * evita trabalho desnecessário no caminho feliz, e a constraint é a garantia real contra a
 * corrida entre duas entregas do mesmo evento processadas em paralelo.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional
    public void processar(ConsultaEvent evento) {
        if (notificacaoRepository.existsByEventoId(evento.eventoId())) {
            log.info("Evento já processado, ignorando: eventoId={}", evento.eventoId());
            return;
        }

        Notificacao notificacao = Notificacao.paraLembreteDeConsulta(
                evento.eventoId(), evento.consultaId(), evento.pacienteId(), montarMensagem(evento));

        try {
            notificacaoRepository.save(notificacao);
        } catch (DataIntegrityViolationException ex) {
            log.info("Evento já processado por outra instância concorrente, ignorando: eventoId={}", evento.eventoId());
            return;
        }

        log.info("Lembrete enviado: pacienteId={} consultaId={} canal={}",
                evento.pacienteId(), evento.consultaId(), notificacao.getCanal());
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> listar(UUID pacienteIdFiltro, AuthenticatedUser autor) {
        UUID filtroEfetivo = autor.isPaciente() ? autor.id() : pacienteIdFiltro;

        List<Notificacao> notificacoes = filtroEfetivo != null
                ? notificacaoRepository.findByPacienteId(filtroEfetivo)
                : notificacaoRepository.findAll();

        return notificacoes.stream().map(NotificacaoResponse::de).collect(Collectors.toList());
    }

    private String montarMensagem(ConsultaEvent evento) {
        String acao = evento.tipo() == TipoEventoConsulta.CRIADA ? "agendada" : "atualizada";
        return "Sua consulta foi %s para %s.".formatted(acao, evento.dataHora());
    }
}
