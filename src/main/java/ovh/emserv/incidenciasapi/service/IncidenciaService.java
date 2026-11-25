package ovh.emserv.incidenciasapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovh.emserv.incidenciasapi.DTO.IncidenciaRequest;
import ovh.emserv.incidenciasapi.DTO.IncidenciaResponse;
import ovh.emserv.incidenciasapi.entity.EstadoIncidencia;
import ovh.emserv.incidenciasapi.entity.Incidencia;
import ovh.emserv.incidenciasapi.entity.Roles;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.exception.AccesoProhibidoException;
import ovh.emserv.incidenciasapi.exception.RecursoNoEncontradoException;
import ovh.emserv.incidenciasapi.repository.IncidenciaRepository;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

import java.util.List;

/**
 * Lógica de negocio de las incidencias.
 *
 * Aquí está el "reto principal": la identidad del usuario sale SIEMPRE del
 * objeto Authentication (que Spring Security construye a partir del JWT), nunca
 * de un id que envíe el cliente. Así, cambiar el número de la URL no da acceso
 * a la incidencia de otra persona.
 */
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Listado de incidencias:
     *  - ROLE_TECNICO y ROLE_ADMIN ven todas.
     *  - ROLE_USER ve solamente las que ha creado él.
     */
    @Transactional(readOnly = true)
    public List<IncidenciaResponse> listar(Authentication autenticacion) {
        List<Incidencia> incidencias = esGestor(autenticacion)
                ? incidenciaRepository.findAllByOrderByFechaCreacionDesc()
                : incidenciaRepository.findByAutorEmailIgnoreCaseOrderByFechaCreacionDesc(
                        autenticacion.getName());

        return incidencias.stream()
                .map(IncidenciaResponse::desde)
                .toList();
    }

    /**
     * Consulta una incidencia concreta comprobando su propiedad.
     *
     * @throws AccesoProhibidoException (403) si es un ROLE_USER y la incidencia
     *         pertenece a otra persona
     */
    @Transactional(readOnly = true)
    public IncidenciaResponse obtener(Long id, Authentication autenticacion) {
        Incidencia incidencia = buscar(id);
        exigirAcceso(incidencia, autenticacion);
        return IncidenciaResponse.desde(incidencia);
    }

    /**
     * Crea una incidencia para el usuario autenticado.
     *
     * El autor se obtiene del email del token (sub) buscándolo en la base de
     * datos. El cuerpo de la petición solo trae título y descripción: si el
     * cliente envía "autor" o "estado", se ignoran.
     */
    @Transactional
    public IncidenciaResponse crear(IncidenciaRequest datos, Authentication autenticacion) {
        Usuario autor = usuarioRepository.findByEmailIgnoreCase(autenticacion.getName())
                .orElseThrow(() -> new AccesoProhibidoException(
                        "El usuario del token ya no existe en la base de datos"));

        Incidencia incidencia = Incidencia.builder()
                .titulo(datos.titulo().trim())
                .descripcion(datos.descripcion().trim())
                .estado(EstadoIncidencia.ABIERTA)
                .autor(autor)
                .build();

        return IncidenciaResponse.desde(incidenciaRepository.saveAndFlush(incidencia));
    }

    /**
     * Cambia el estado de una incidencia. Las rutas ya exigen ROLE_TECNICO o
     * ROLE_ADMIN; la comprobación se repite aquí para no depender solo de la
     * configuración de rutas.
     */
    @Transactional
    public IncidenciaResponse cambiarEstado(Long id, EstadoIncidencia estado, Authentication autenticacion) {
        if (!esGestor(autenticacion)) {
            throw new AccesoProhibidoException(
                    "Solo un técnico o un administrador puede cambiar el estado de una incidencia");
        }

        Incidencia incidencia = buscar(id);
        incidencia.setEstado(estado);

        return IncidenciaResponse.desde(incidenciaRepository.saveAndFlush(incidencia));
    }

    /**
     * Elimina una incidencia. Solo ROLE_ADMIN (comprobado en la ruta, en
     * @PreAuthorize y aquí).
     */
    @Transactional
    public void eliminar(Long id, Authentication autenticacion) {
        if (!tieneRol(autenticacion, Roles.ADMIN)) {
            throw new AccesoProhibidoException("Solo un administrador puede eliminar una incidencia");
        }

        incidenciaRepository.delete(buscar(id));
    }

    /** Busca la incidencia o lanza 404. */
    private Incidencia buscar(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la incidencia con id " + id));
    }

    /**
     * Regla del reto: un ROLE_USER solo puede acceder a sus propias incidencias.
     * TECNICO y ADMIN pueden acceder a cualquiera.
     */
    private void exigirAcceso(Incidencia incidencia, Authentication autenticacion) {
        if (esGestor(autenticacion)) {
            return;
        }
        if (!esAutor(incidencia, autenticacion.getName())) {
            throw new AccesoProhibidoException(
                    "No puedes consultar una incidencia de otro usuario");
        }
    }

    private boolean esAutor(Incidencia incidencia, String email) {
        return incidencia.getAutor().getEmail().equalsIgnoreCase(email);
    }

    private boolean esGestor(Authentication autenticacion) {
        return tieneRol(autenticacion, Roles.TECNICO)
                || tieneRol(autenticacion, Roles.ADMIN);
    }

    private boolean tieneRol(Authentication autenticacion, String rol) {
        if (autenticacion == null || autenticacion.getAuthorities() == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol::equals);
    }
}
