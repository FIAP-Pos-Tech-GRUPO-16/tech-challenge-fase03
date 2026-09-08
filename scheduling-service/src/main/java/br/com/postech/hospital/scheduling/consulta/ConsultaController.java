package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/consultas")
@Tag(name = "Consultas", description = "Agendamento, edição e consulta de consultas médicas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    @Operation(
            summary = "Registra uma nova consulta",
            description = "Cria a consulta com status AGENDADA e publica um evento assíncrono " +
                    "para o serviço de notificações enviar um lembrete ao paciente. Restrito a médicos e enfermeiros."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consulta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: data no passado)"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é médico nem enfermeiro")
    })
    public ResponseEntity<ConsultaResponse> criar(@Valid @RequestBody ConsultaRequest request) {
        ConsultaResponse response = consultaService.criar(request, AuthenticatedUser.current());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    @Operation(
            summary = "Edita uma consulta existente",
            description = "Atualiza data/hora, status e observações, e publica um novo evento " +
                    "assíncrono avisando sobre a alteração. Restrito a médicos e enfermeiros."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta atualizada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é médico nem enfermeiro"),
            @ApiResponse(responseCode = "404", description = "Consulta não encontrada")
    })
    public ResponseEntity<ConsultaResponse> editar(
            @Parameter(description = "Id da consulta") @PathVariable UUID id,
            @Valid @RequestBody ConsultaUpdateRequest request) {
        return ResponseEntity.ok(consultaService.editar(id, request, AuthenticatedUser.current()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    @Operation(
            summary = "Busca uma consulta pelo id",
            description = "Médicos e enfermeiros podem buscar qualquer consulta. Um paciente só " +
                    "consegue buscar uma consulta que seja sua — caso contrário recebe 403."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta encontrada"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Paciente tentando acessar consulta de outra pessoa"),
            @ApiResponse(responseCode = "404", description = "Consulta não encontrada")
    })
    public ResponseEntity<ConsultaResponse> buscarPorId(
            @Parameter(description = "Id da consulta") @PathVariable UUID id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id, AuthenticatedUser.current()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    @Operation(
            summary = "Lista consultas",
            description = "Médicos e enfermeiros podem listar todas as consultas ou filtrar por " +
                    "paciente. Um paciente autenticado sempre recebe apenas as próprias consultas — " +
                    "o filtro informado é ignorado e substituído pelo id do próprio usuário."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de consultas (pode ser vazia)"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado")
    })
    public ResponseEntity<List<ConsultaResponse>> listar(
            @Parameter(description = "Filtra por paciente (ignorado quando quem chama é um paciente)")
            @RequestParam(required = false) UUID pacienteId) {
        return ResponseEntity.ok(consultaService.listar(pacienteId, AuthenticatedUser.current()));
    }
}
