package ovh.emserv.incidenciasapi.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del registro público de usuarios.
 *
 * No tiene campo "rol": aunque el cliente envíe un rol en el JSON, Jackson lo
 * ignora y el servicio asigna siempre ROLE_USER.
 */
public record RegistroRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password
) {
}
