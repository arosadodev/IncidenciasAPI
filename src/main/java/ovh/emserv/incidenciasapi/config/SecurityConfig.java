package ovh.emserv.incidenciasapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import ovh.emserv.incidenciasapi.service.JwtService;

/**
 * Configuración de seguridad de la API.
 *
 * Rutas públicas y permisos por rol:
 *  - /auth/** públicos (registro y login).
 *  - Crear y consultar incidencias: cualquier usuario autenticado. El filtrado
 *    por autor se hace en IncidenciaService con la identidad del token.
 *  - Cambiar el estado: ROLE_TECNICO o ROLE_ADMIN.
 *  - Eliminar incidencias y todo /usuarios/**: ROLE_ADMIN.
 *
 * Sin sesiones: cada petición se valida con su JWT (STATELESS).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Deshabilitar CSRF (no necesario en APIs REST con JWT)
                .csrf(csrf -> csrf.disable())

                // Autorización de las peticiones HTTP
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: registro y login
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Incidencias
                        .requestMatchers(HttpMethod.POST, "/incidencias").authenticated()
                        .requestMatchers(HttpMethod.GET, "/incidencias", "/incidencias/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/incidencias/*/estado")
                            .hasAnyRole("TECNICO", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/incidencias/*").hasRole("ADMIN")

                        // Usuarios: solo administradores
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                // Validación automática del JWT en cada petición protegida
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())                 // cómo se valida el token
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // cómo se extraen los roles
                        )
                )

                // Sin sesiones en el servidor
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * Cómo se valida el token: firma HMAC con la clave de las propiedades,
     * caducidad y emisor. Si algo no cuadra, la petición acaba en 401.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtService.getSecretKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        return decoder;
    }

    /**
     * Cómo se extraen los roles del token: del claim "roles", que ya guarda
     * valores con el prefijo ROLE_ (por eso el prefijo del converter es "").
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return jwtConverter;
    }

    /** BCrypt para cifrar las contraseñas en el registro y compararlas en el login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Necesario para autenticar email/contraseña en /auth/login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
