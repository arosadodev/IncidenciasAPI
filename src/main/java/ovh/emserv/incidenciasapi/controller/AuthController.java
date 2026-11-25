package ovh.emserv.incidenciasapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ovh.emserv.incidenciasapi.DTO.ErrorResponse;
import ovh.emserv.incidenciasapi.DTO.LoginRequest;
import ovh.emserv.incidenciasapi.DTO.LoginResponse;
import ovh.emserv.incidenciasapi.DTO.RegistroRequest;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.entity.Usuario;
import ovh.emserv.incidenciasapi.exception.RecursoNoEncontradoException;
import ovh.emserv.incidenciasapi.repository.UsuarioRepository;
import ovh.emserv.incidenciasapi.service.AuthService;
import ovh.emserv.incidenciasapi.service.JwtService;

/**
 * Registro público y login. Son las dos únicas rutas abiertas.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    /**
     * POST /auth/register -> 201 con los datos públicos del usuario creado.
     * Siempre se crea con ROLE_USER y la contraseña cifrada con BCrypt.
     */
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(datos));
    }

    /**
     * POST /auth/login -> 200 con el JWT o 401 si las credenciales no son correctas.
     *
     * AuthenticationManager comprueba email + contraseña con BCrypt usando
     * CustomUserDetailsService. Si todo va bien, se genera el token con los
     * datos del usuario de la base de datos.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest datos) {
        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(datos.email(), datos.password()));

            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(autenticacion.getName())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el usuario autenticado"));

            String token = jwtService.generarToken(usuario);

            return ResponseEntity.ok(new LoginResponse(
                    token,
                    "Bearer",
                    jwtService.getCaducidadMs() / 1000,
                    UsuarioResponse.desde(usuario)));

        } catch (AuthenticationException e) {
            // Usuario inexistente, contraseña incorrecta o cuenta desactivada
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Credenciales incorrectas"));
        }
    }
}
