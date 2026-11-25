package ovh.emserv.incidenciasapi.exception;

/**
 * Se lanza cuando un usuario autenticado intenta operar sobre un recurso que
 * no le pertenece y su rol no se lo permite (por ejemplo, un ROLE_USER que
 * pide por URL una incidencia de otro autor).
 *
 * Importante: es una excepción propia para no confundirla con
 * org.springframework.security.access.AccessDeniedException. El 403 de este
 * caso lo genera el manejador global; el de @PreAuthorize lo genera
 * ExceptionTranslationFilter con el AccessDeniedHandler de SecurityConfig.
 */
public class AccesoProhibidoException extends RuntimeException {

    public AccesoProhibidoException(String mensaje) {
        super(mensaje);
    }
}
