package ovh.emserv.incidenciasapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de la capa de negocio para consultar usuarios y cambiar su rol.
 * Las rutas que lo usan solo son accesibles para ROLE_ADMIN.
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
     * Cambia el rol de un usuario. La lista blanca de roles se comprueba en
     * UsuarioController; aquí solo se comprueba que el usuario existe.
     */
    @Transactional
    public Optional<UsuarioResponse> cambiarRol(Long id, String rol) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        usuario.get().setRol(rol);

        return Optional.of(UsuarioResponse.desde(usuarioRepository.saveAndFlush(usuario.get())));
    }
}
