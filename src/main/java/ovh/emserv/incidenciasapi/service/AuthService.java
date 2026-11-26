package ovh.emserv.incidenciasapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovh.emserv.incidenciasapi.DTO.UsuarioRequest;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

import java.util.Locale;

/**
 * Servicio de la capa de negocio que registra a los usuarios nuevos.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea el usuario con ROLE_USER y la contraseña cifrada con BCrypt.
     * El rol no se lee del JSON: UsuarioRequest no tiene ese campo.
     */
    @Transactional
    public UsuarioResponse registrar(UsuarioRequest datos) {
        Usuario usuario = Usuario.builder()
                .email(datos.email().trim().toLowerCase(Locale.ROOT))
                .nombre(datos.nombre().trim())
                .password(passwordEncoder.encode(datos.password()))
                .rol("ROLE_USER")
                .activo(true)
                .build();

        return UsuarioResponse.desde(usuarioRepository.saveAndFlush(usuario));
    }
}
