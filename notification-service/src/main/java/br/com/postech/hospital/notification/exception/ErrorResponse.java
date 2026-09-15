package br.com.postech.hospital.notification.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mesmo formato de erro do serviço de agendamento, mantido aqui em vez de compartilhado num
 * módulo comum: os serviços são deployáveis independentes, e acoplar o contrato de erro dos três
 * num artefato único obrigaria a reconstruir todo mundo para mudar uma mensagem. A duplicação de
 * um record de cinco campos é mais barata que esse acoplamento.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> detalhes
) {

    public static ErrorResponse de(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, List.of());
    }
}
