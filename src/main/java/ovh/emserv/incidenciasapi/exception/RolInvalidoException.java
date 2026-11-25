package ovh.emserv.incidenciasapi.exception;

/**
 * Se lanza cuando el rol enviado al cambiar permisos no es uno de los admitidos.
 * El manejador global la convierte en 400 Bad Request.
 */
public class RolInvalidoException extends RuntimeException {

    public RolInvalidoException(String mensaje) {
        super(mensaje);
    }
}
