package ovh.emserv.incidenciasapi.DTO;

import ovh.emserv.incidenciasapi.entity.EstadoIncidencia;
import ovh.emserv.incidenciasapi.entity.Incidencia;

import java.time.LocalDateTime;

/**
 * Representación de una incidencia en las respuestas de la API.
 */
public record IncidenciaResponse(
        Long id,
        String titulo,
        String descripcion,
        EstadoIncidencia estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        AutorResponse autor
) {

    /** Convierte la entidad en DTO mientras la sesión JPA sigue abierta. */
    public static IncidenciaResponse desde(Incidencia incidencia) {
        return new IncidenciaResponse(
                incidencia.getId(),
                incidencia.getTitulo(),
                incidencia.getDescripcion(),
                incidencia.getEstado(),
                incidencia.getFechaCreacion(),
                incidencia.getFechaActualizacion(),
                new AutorResponse(
                        incidencia.getAutor().getId(),
                        incidencia.getAutor().getNombre(),
                        incidencia.getAutor().getEmail()
                )
        );
    }
}
