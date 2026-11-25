package ovh.emserv.incidenciasapi.exception;

/**
 * Se lanza cuando el email de registro ya pertenece a otro usuario.
 * El manejador global la convierte en 409 Conflict.
 */
public class EmailRepetidoException extends RuntimeException {

    public EmailRepetidoException() {
        super("El email ya está registrado");
    }
}
