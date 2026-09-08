package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.security.AuthenticatedUser;
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
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ResponseEntity<ConsultaResponse> criar(@Valid @RequestBody ConsultaRequest request) {
        ConsultaResponse response = consultaService.criar(request, AuthenticatedUser.current());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO')")
    public ResponseEntity<ConsultaResponse> editar(@PathVariable UUID id, @Valid @RequestBody ConsultaUpdateRequest request) {
        return ResponseEntity.ok(consultaService.editar(id, request, AuthenticatedUser.current()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id, AuthenticatedUser.current()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMEIRO', 'PACIENTE')")
    public ResponseEntity<List<ConsultaResponse>> listar(@RequestParam(required = false) UUID pacienteId) {
        return ResponseEntity.ok(consultaService.listar(pacienteId, AuthenticatedUser.current()));
    }
}
