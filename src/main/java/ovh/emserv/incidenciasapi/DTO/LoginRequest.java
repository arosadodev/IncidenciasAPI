package ovh.emserv.incidenciasapi.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales del login.
 */
public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
