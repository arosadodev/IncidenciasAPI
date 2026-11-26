package ovh.emserv.incidenciasapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovh.emserv.incidenciasapi.DTO.IncidenciaRequest;
import ovh.emserv.incidenciasapi.DTO.IncidenciaResponse;
import ovh.emserv.incidenciasapi.entity.EstadoIncidencia;
import ovh.emserv.incidenciasapi.entity.Incidencia;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.repository.IncidenciaRepository;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de la capa de negocio de las incidencias.
 *
 * Aquí está el reto principal: la identidad del usuario sale SIEMPRE del objeto
 * Authentication (construido a partir del JWT), nunca de un id del cliente.
 */
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * ROLE_USER ve solo sus incidencias; ROLE_TECNICO y ROLE_ADMIN ven todas.
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
     * Consulta una incidencia concreta comprobando que pertenece al usuario,
     * salvo que sea TECNICO o ADMIN. Si es de otro autor lanza AccessDeniedException,
     * que Spring Security convierte en 403.
     */
    @Transactional(readOnly = true)
    public Optional<IncidenciaResponse> obtener(Long id, Authentication autenticacion) {
        Optional<Incidencia> incidencia = incidenciaRepository.findById(id);
        if (incidencia.isEmpty()) {
            return Optional.empty();
        }

        exigirAcceso(incidencia.get(), autenticacion);

        return Optional.of(IncidenciaResponse.desde(incidencia.get()));
    }

    /**
     * Crea la incidencia con el autor obtenido de la autenticación.
     * El cuerpo de la petición solo trae título y descripción.
     */
    @Transactional
    public IncidenciaResponse crear(IncidenciaRequest datos, Authentication autenticacion) {
        Usuario autor = usuarioRepository.findByEmailIgnoreCase(autenticacion.getName())
                .orElseThrow(() -> new AccessDeniedException("El usuario del token ya no existe"));

        Incidencia incidencia = Incidencia.builder()
                .titulo(datos.titulo().trim())
                .descripcion(datos.descripcion().trim())
                .estado(EstadoIncidencia.ABIERTA)
                .autor(autor)
                .build();

        return IncidenciaResponse.desde(incidenciaRepository.saveAndFlush(incidencia));
    }

    /**
     * Cambia el estado de una incidencia. La ruta ya exige TECNICO o ADMIN.
     */
    @Transactional
    public Optional<IncidenciaResponse> cambiarEstado(Long id, EstadoIncidencia estado) {
        Optional<Incidencia> incidencia = incidenciaRepository.findById(id);
        if (incidencia.isEmpty()) {
            return Optional.empty();
        }

        incidencia.get().setEstado(estado);

        return Optional.of(IncidenciaResponse.desde(incidenciaRepository.saveAndFlush(incidencia.get())));
    }

    /**
     * Elimina una incidencia. La ruta ya exige ROLE_ADMIN.
     */
    @Transactional
    public boolean eliminar(Long id) {
        if (!incidenciaRepository.existsById(id)) {
            return false;
        }
        incidenciaRepository.deleteById(id);
        return true;
    }

    /**
     * Regla del reto: un ROLE_USER solo puede acceder a sus propias incidencias.
     */
    private void exigirAcceso(Incidencia incidencia, Authentication autenticacion) {
        if (esGestor(autenticacion)) {
            return;
        }
        if (!incidencia.getAutor().getEmail().equalsIgnoreCase(autenticacion.getName())) {
            throw new AccessDeniedException("No puedes consultar una incidencia de otro usuario");
        }
    }

    private boolean esGestor(Authentication autenticacion) {
        return tieneRol(autenticacion, "ROLE_TECNICO") || tieneRol(autenticacion, "ROLE_ADMIN");
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
