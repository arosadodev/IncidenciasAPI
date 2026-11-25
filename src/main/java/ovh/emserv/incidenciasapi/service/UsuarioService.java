package ovh.emserv.incidenciasapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.entity.Roles;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.exception.RecursoNoEncontradoException;
import ovh.emserv.incidenciasapi.exception.RolInvalidoException;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

import java.util.List;
import java.util.TreeSet;

/**
 * Operaciones de administración sobre usuarios: listarlos y cambiar su rol.
 * Las rutas que las usan solo son accesibles para ROLE_ADMIN.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    /**
     * Cambia el rol de un usuario validando que sea uno de los admitidos.
     * Si el valor no está en la lista blanca se lanza RolInvalidoException (400)
     * y si el usuario no existe, RecursoNoEncontradoException (404).
     *
     * Nota: el cambio en la base de datos es inmediato, pero el token que ya
     * tuviera el usuario sigue llevando el rol antiguo hasta que caduque o
     * vuelva a iniciar sesión (ver README, apartado "Para pensar").
     */
    @Transactional
    public UsuarioResponse cambiarRol(Long id, String rol) {
        String rolSolicitado = rol == null ? "" : rol.trim();

        if (!Roles.VALIDOS.contains(rolSolicitado)) {
            throw new RolInvalidoException(
                    "Rol no válido. Valores admitidos: " + String.join(", ", new TreeSet<>(Roles.VALIDOS)));
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));

        usuario.setRol(rolSolicitado);
        usuarioRepository.save(usuario);

        return UsuarioResponse.desde(usuario);
    }
}
