package ovh.emserv.incidenciasapi.DTO;

/**
 * Cuerpo JSON que devuelve la API cuando una operación falla.
 * Se usa tanto en el manejador global de excepciones como en los
 * manejadores de seguridad (401 y 403).
 */
public record ErrorResponse(
        int estado,
        String mensaje
) {
}
