package ovh.emserv.incidenciasapi.entity;

import java.util.Set;

/**
 * Nombres de los roles admitidos por la aplicación.
 *
 * Se centralizan aquí para que el registro, la administración de usuarios y las
 * comprobaciones de permisos no repitan cadenas sueltas. Cualquier otro valor
 * enviado al cambiar un rol se rechaza con 400.
 */
public final class Roles {

    public static final String USER = "ROLE_USER";
    public static final String TECNICO = "ROLE_TECNICO";
    public static final String ADMIN = "ROLE_ADMIN";

    /** Lista blanca de roles válidos. */
    public static final Set<String> VALIDOS = Set.of(USER, TECNICO, ADMIN);

    private Roles() {
    }
}
