package ovh.emserv.incidenciasapi.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ovh.emserv.incidenciasapi.entity.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Servicio de la capa de negocio que genera y firma los tokens JWT.
 *
 * La clave se lee de application.properties (app.jwt.secret) para que los
 * tokens sigan siendo válidos al reiniciar la aplicación.
 */
@Service
public class JwtService {

    // Emisor que se incluye en el token y que valida el decodificador.
    public static final String EMISOR = "App-Incidencias";

    private final SecretKey secretKey;
    private final long caducidadMs;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-ms}") long caducidadMs) {

        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 bytes para firmar con HS256");
        }
        // Clave HMAC a partir del valor de la propiedad.
        this.secretKey = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.caducidadMs = caducidadMs;
    }

    /**
     * Genera un token para el usuario autenticado con:
     *  - sub: email del usuario
     *  - id: identificador del usuario
     *  - roles: lista de roles
     *  - nombre: claim adicional NO sensible
     *  - emisor, fecha de emisión y fecha de caducidad
     */
    public String generarToken(Usuario usuario) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + caducidadMs))
                .claim("id", usuario.getId())
                .claim("roles", List.of(usuario.getRol()))
                .claim("nombre", usuario.getNombre())
                // Se fija HS256: con una clave de 64 bytes jjwt elegiría HS512 y el
                // decodificador (configurado para HS256) rechazaría el token.
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}
