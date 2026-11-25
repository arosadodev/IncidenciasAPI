package ovh.emserv.incidenciasapi.DTO;

import ovh.emserv.incidenciasapi.entity.Usuario;

import java.time.LocalDateTime;

/**
 * Datos públicos de un usuario. Nunca incluye la contraseña.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        Boolean activo,
        LocalDateTime fechaRegistro
) {

    /** Convierte la entidad en DTO para no exponer la contraseña cifrada. */
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaRegistro()
        );
    }
}
