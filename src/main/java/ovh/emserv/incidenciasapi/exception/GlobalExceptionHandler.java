package ovh.emserv.incidenciasapi.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores de la API.
 *
 * No hay ningún manejador genérico de Exception a propósito: capturaría
 * AccessDeniedException (por ejemplo, la de una incidencia ajena) y devolvería
 * un 500 en lugar del 403 que genera Spring Security.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Errores de @Valid: devuelve un mapa campo -> mensaje con 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }
}
