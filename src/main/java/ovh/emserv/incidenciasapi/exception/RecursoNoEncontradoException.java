package ovh.emserv.incidenciasapi.exception;

/**
 * Se lanza cuando el recurso solicitado no existe (por ejemplo una incidencia
 * o un usuario con un id desconocido). El manejador global la convierte en 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
