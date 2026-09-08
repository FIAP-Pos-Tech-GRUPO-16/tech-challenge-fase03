package br.com.postech.hospital.notification.notificacao;

import br.com.postech.hospital.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notificacoes")
@Tag(name = "Notificações", description = "Lembretes enviados aos pacientes sobre suas consultas")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    @Operation(
            summary = "Lista os lembretes já enviados",
            description = "Um lembrete é criado automaticamente, de forma assíncrona, sempre que " +
                    "uma consulta é criada ou editada no serviço de agendamento. Médicos e " +
                    "enfermeiros podem listar todos ou filtrar por paciente; um paciente " +
                    "autenticado sempre recebe apenas os próprios lembretes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notificações (pode ser vazia)"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado")
    })
    public ResponseEntity<List<NotificacaoResponse>> listar(
            @Parameter(description = "Filtra por paciente (ignorado quando quem chama é um paciente)")
            @RequestParam(required = false) UUID pacienteId) {
        return ResponseEntity.ok(notificacaoService.listar(pacienteId, AuthenticatedUser.current()));
    }
}
