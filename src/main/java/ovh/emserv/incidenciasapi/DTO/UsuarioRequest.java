package ovh.emserv.incidenciasapi.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada con los datos del registro público.
 *
 * No tiene campo "rol" a propósito: aunque el cliente lo envíe en el JSON,
 * Jackson lo ignora y el servicio asigna siempre ROLE_USER.
 */
public record UsuarioRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password
) {
}
