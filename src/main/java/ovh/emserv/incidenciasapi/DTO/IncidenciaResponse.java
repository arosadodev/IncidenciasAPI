package ovh.emserv.incidenciasapi.DTO;

import ovh.emserv.incidenciasapi.entity.EstadoIncidencia;
import ovh.emserv.incidenciasapi.entity.Incidencia;

import java.time.LocalDateTime;

/**
 * DTO de salida con los datos de una incidencia y de su autor.
 */
public record IncidenciaResponse(
        Long id,
        String titulo,
        String descripcion,
        EstadoIncidencia estado,
        LocalDateTime fechaCreacion,
        UsuarioResponse autor
) {

    // Convierte la entidad en DTO mientras la sesión JPA sigue abierta.
    public static IncidenciaResponse desde(Incidencia incidencia) {
        return new IncidenciaResponse(
                incidencia.getId(),
                incidencia.getTitulo(),
                incidencia.getDescripcion(),
                incidencia.getEstado(),
                incidencia.getFechaCreacion(),
                UsuarioResponse.desde(incidencia.getAutor())
        );
    }
}
