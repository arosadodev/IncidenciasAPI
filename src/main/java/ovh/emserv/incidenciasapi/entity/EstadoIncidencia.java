package ovh.emserv.incidenciasapi.entity;

/**
 * Estados por los que pasa una incidencia del centro.
 * Se guardan en la base de datos como texto (@Enumerated(EnumType.STRING)).
 */
public enum EstadoIncidencia {
    ABIERTA,
    EN_PROCESO,
    RESUELTA
}
