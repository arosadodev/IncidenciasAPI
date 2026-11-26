package ovh.emserv.incidenciasapi.DTO;

import ovh.emserv.incidenciasapi.entity.Usuario;

/**
 * DTO de salida con los datos públicos de un usuario. Nunca incluye la contraseña.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol
) {

    // Convierte la entidad en DTO para no exponer la contraseña cifrada.
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol()
        );
    }
}
