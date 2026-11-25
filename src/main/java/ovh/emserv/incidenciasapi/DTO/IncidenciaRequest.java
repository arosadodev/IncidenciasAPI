package ovh.emserv.incidenciasapi.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una incidencia.
 *
 * No incluye autor ni estado a propósito: el autor sale de la autenticación y
 * el estado inicial (ABIERTA) lo decide el servidor.
 */
public record IncidenciaRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120, message = "El título no puede superar 120 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion
) {
}
