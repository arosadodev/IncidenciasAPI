package ovh.emserv.incidenciasapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 * Resumen de la política:
 *  - /auth/** es público (registro y login).
 *  - Todo lo demás requiere un JWT válido.
 *  - Cambiar el estado de una incidencia: ROLE_TECNICO o ROLE_ADMIN.
 *  - Eliminar una incidencia: ROLE_ADMIN.
 *  - Consultar/modificar usuarios: ROLE_ADMIN.
 *  - El resto (crear y consultar incidencias) requiere estar autenticado; el
 *    filtrado por autor se hace en IncidenciaService con la identidad del token.
 *
 * No hay sesiones: cada petición se autentica con su token (STATELESS).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // activa @PreAuthorize en los controladores
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Sin formularios ni cookies de sesión no hace falta CSRF
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // ---------- Rutas públicas ----------
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ---------- Incidencias ----------
                        // Crear: cualquier usuario autenticado (USER, TECNICO, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/incidencias").authenticated()
                        // Consultar listado o una incidencia concreta: autenticado.
                        // El "solo mis incidencias" se comprueba en el servicio.
                        .requestMatchers(HttpMethod.GET, "/incidencias", "/incidencias/*").authenticated()
                        // Cambiar el estado: solo TECNICO y ADMIN
                        .requestMatchers(HttpMethod.PATCH, "/incidencias/*/estado")
                        .hasAnyRole("TECNICO", "ADMIN")
                        // Eliminar: solo ADMIN
                        .requestMatchers(HttpMethod.DELETE, "/incidencias/*").hasRole("ADMIN")

                        // ---------- Usuarios ----------
                        // Consultar usuarios y cambiar roles: solo ADMIN
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                // Validación automática del JWT en cada petición protegida
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                // 401 sin token / token inválido y 403 sin permisos, con cuerpo JSON
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )

                // Sin sesiones en servidor: el estado viaja en el token
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
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtService.getClave()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtService.getEmisor()));
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
