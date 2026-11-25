package ovh.emserv.incidenciasapi.DTO;

/**
 * Datos mínimos del autor que se incluyen en la respuesta de una incidencia.
 */
public record AutorResponse(
        Long id,
        String nombre,
        String email
) {
}
