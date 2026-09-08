package br.com.postech.hospital.scheduling.exception;

import java.time.LocalDateTime;
import java.util.List;

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

    public static ErrorResponse de(int status, String error, String message, List<String> detalhes) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, detalhes);
    }
}
