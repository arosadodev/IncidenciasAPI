package ovh.emserv.incidenciasapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;

/**
 * Datos iniciales de la aplicación: se comprueban en cada arranque.
 *
 * Deja siempre creados estos usuarios de prueba (contraseña: 1234):
 *
 *   admin@centro.es     -> ROLE_ADMIN     (usuario: admin@centro.es   / contraseña: 1234)
 *   tecnico@centro.es   -> ROLE_TECNICO   (usuario: tecnico@centro.es / contraseña: 1234)
 *   ana@centro.es       -> ROLE_USER      (usuario: ana@centro.es     / contraseña: 1234)
 *   luis@centro.es      -> ROLE_USER      (usuario: luis@centro.es    / contraseña: 1234)
 *
 * La contraseña se guarda YA CIFRADA con BCrypt: en el código solo está el
 * hash (el de "1234"), nunca la contraseña en claro en la base de datos.
 * Si los usuarios ya existen, se actualizan su rol, su estado y su contraseña
 * para que los datos de prueba sean siempre los mismos.
 */
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    // Hash BCrypt de la contraseña "1234" (mismo para los cuatro usuarios de prueba).
    private static final String PASSWORD_CIFRADA = "$2a$10$zi8cpoYklR0v5GIfbMzmnuZHa0fy1ttJVSLcKpKOFFbpM9MXaOoYO";

    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        crearOActualizar("admin@centro.es", "Administrador", "ROLE_ADMIN");
        crearOActualizar("tecnico@centro.es", "Tecnico", "ROLE_TECNICO");
        crearOActualizar("ana@centro.es", "Ana", "ROLE_USER");
        crearOActualizar("luis@centro.es", "Luis", "ROLE_USER");
    }

    private void crearOActualizar(String email, String nombre, String rol) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseGet(Usuario::new);

        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setPassword(PASSWORD_CIFRADA);
        usuario.setRol(rol);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);
    }
}
