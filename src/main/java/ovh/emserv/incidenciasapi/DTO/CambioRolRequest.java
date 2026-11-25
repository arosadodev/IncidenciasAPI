package ovh.emserv.incidenciasapi.DTO;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de PATCH /usuarios/{id}/rol.
 *
 * Se recibe como String (y no como enum) para poder devolver un 400 con un
 * mensaje claro cuando el rol no es uno de los admitidos.
 */
public record CambioRolRequest(
        @NotBlank(message = "El rol es obligatorio")
        String rol
) {
}
