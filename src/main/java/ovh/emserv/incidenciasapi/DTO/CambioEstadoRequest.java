package ovh.emserv.incidenciasapi.DTO;

import jakarta.validation.constraints.NotNull;
import ovh.emserv.incidenciasapi.entity.EstadoIncidencia;

/**
 * Cuerpo de PATCH /incidencias/{id}/estado.
 * Si se envía un valor que no es ABIERTA, EN_PROCESO o RESUELTA,
 * Jackson lanza un error de lectura y el manejador global devuelve 400.
 */
public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoIncidencia estado
) {
}
