package ovh.emserv.incidenciasapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovh.emserv.incidenciasapi.DTO.RegistroRequest;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.entity.Roles;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.exception.EmailRepetidoException;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

import java.util.Locale;

/**
 * Registro de nuevos usuarios.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un usuario nuevo con ROLE_USER.
     *
     * El rol no se lee del JSON: RegistroRequest no tiene ese campo, así que
     * un cliente que envíe "rol": "ROLE_ADMIN" no consigue privilegios.
     * La contraseña se guarda cifrada con BCrypt.
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        String email = datos.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailRepetidoException();
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .nombre(datos.nombre().trim())
                .password(passwordEncoder.encode(datos.password()))
                .rol(Roles.USER)
                .activo(true)
                .build();

        return UsuarioResponse.desde(usuarioRepository.saveAndFlush(usuario));
    }
}
