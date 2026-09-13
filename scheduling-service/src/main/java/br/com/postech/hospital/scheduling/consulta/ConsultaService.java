package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.scheduling.exception.ResourceNotFoundException;
import br.com.postech.hospital.scheduling.messaging.ConsultaAlteradaEvent;
import br.com.postech.hospital.scheduling.usuario.UsuarioRepository;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Regras de negócio do agendamento. Criação e edição são permitidas para médicos e enfermeiros
 * (enforçado por {@code @PreAuthorize} no controller); a leitura é liberada aos três papéis,
 * mas um paciente só enxerga as próprias consultas — essa checagem de posse só pode acontecer
 * aqui, depois de carregar o dado, pois a anotação de segurança não conhece o dono do registro.
 */
@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ConsultaService(ConsultaRepository consultaRepository,
                           UsuarioRepository usuarioRepository,
                           ApplicationEventPublisher applicationEventPublisher) {
        this.consultaRepository = consultaRepository;
        this.usuarioRepository = usuarioRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public ConsultaResponse criar(ConsultaRequest request, AuthenticatedUser autor) {
        exigirUsuarioComPapel(request.pacienteId(), SecurityRole.PACIENTE, "pacienteId");
        exigirUsuarioComPapel(request.medicoId(), SecurityRole.MEDICO, "medicoId");

        Consulta consulta = Consulta.agendar(request.pacienteId(), request.medicoId(), autor.id(),
                request.dataHora(), request.observacoes());
        consultaRepository.save(consulta);
        applicationEventPublisher.publishEvent(new ConsultaAlteradaEvent(consulta, TipoEventoConsulta.CRIADA));
        return ConsultaResponse.de(consulta);
    }

    @Transactional
    public ConsultaResponse editar(UUID id, ConsultaUpdateRequest request) {
        Consulta consulta = buscarOuFalhar(id);
        consulta.atualizar(request.dataHora(), request.status(), request.observacoes());
        applicationEventPublisher.publishEvent(new ConsultaAlteradaEvent(consulta, TipoEventoConsulta.EDITADA));
        return ConsultaResponse.de(consulta);
    }

    @Transactional(readOnly = true)
    public ConsultaResponse buscarPorId(UUID id, AuthenticatedUser autor) {
        Consulta consulta = buscarOuFalhar(id);
        garantirAcesso(consulta, autor);
        return ConsultaResponse.de(consulta);
    }

    @Transactional(readOnly = true)
    public List<ConsultaResponse> listar(UUID pacienteIdFiltro, AuthenticatedUser autor) {
        UUID filtroEfetivo = autor.isPaciente() ? autor.id() : pacienteIdFiltro;

        List<Consulta> consultas = filtroEfetivo != null
                ? consultaRepository.findByPacienteId(filtroEfetivo)
                : consultaRepository.findAll();

        return consultas.stream().map(ConsultaResponse::de).toList();
    }

    private Consulta buscarOuFalhar(UUID id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada: " + id));
    }

    private void garantirAcesso(Consulta consulta, AuthenticatedUser autor) {
        if (autor.isPaciente() && !consulta.pertenceAoPaciente(autor.id())) {
            throw new AccessDeniedException("Paciente só pode acessar as próprias consultas");
        }
    }

    /**
     * Valida que o id informado existe e pertence a alguém com o papel esperado.
     *
     * <p>Sem isto, um id inexistente só era rejeitado lá embaixo, pela foreign key da tabela —
     * o que chegava ao cliente como HTTP 500. E um id existente mas do papel errado (agendar
     * uma consulta cujo "médico" é na verdade um paciente) passava sem nenhuma reclamação.
     *
     * <p>{@link IllegalArgumentException} já é traduzida para HTTP 400 pelo
     * {@code GlobalExceptionHandler}.
     */
    private void exigirUsuarioComPapel(UUID id, SecurityRole papelEsperado, String campo) {
        usuarioRepository.findById(id)
                .filter(usuario -> usuario.getRole() == papelEsperado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "%s não corresponde a um usuário com papel %s".formatted(campo, papelEsperado)));
    }
}
