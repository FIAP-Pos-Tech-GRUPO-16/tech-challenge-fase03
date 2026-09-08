package br.com.postech.hospital.scheduling.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e emissão do token JWT usado por todos os serviços")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Autentica um usuário e retorna o JWT",
            description = """
                    Recebe usuário e senha e devolve um token JWT válido por 120 minutos (padrão).
                    Use os usuários de demonstração descritos no README, ou o cadastro que você
                    criar. O token deve ser enviado no header "Authorization: Bearer <token>" em
                    toda chamada às rotas protegidas — deste serviço, do de notificações e do de
                    histórico.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido (username ou password ausentes)"),
            @ApiResponse(responseCode = "401", description = "Usuário ou senha incorretos")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }
}
