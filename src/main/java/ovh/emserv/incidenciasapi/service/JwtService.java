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
 * Genera y firma los tokens JWT.
 *
 * La clave y la caducidad se leen de application.properties (app.jwt.*), así
 * que al reiniciar con la misma clave los tokens anteriores siguen siendo
 * válidos. Si la clave tuviera menos de 32 bytes, HS256 no es seguro y la
 * aplicación falla al arrancar con un mensaje explicativo.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long caducidadMs;
    private final String emisor;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-ms}") long caducidadMs,
            @Value("${app.jwt.issuer}") String emisor) {

        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos 32 bytes para firmar con HS256");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.caducidadMs = caducidadMs;
        this.emisor = emisor;
    }

    /**
     * Construye el token con:
     *  - sub: email del usuario (identidad que Spring Security usará como getName())
     *  - id: identificador del usuario
     *  - roles: lista de roles (los lee JwtAuthenticationConverter)
     *  - nombre: claim adicional NO sensible, útil para mostrar en el frontend
     *  - iat / exp: fecha de emisión y de caducidad
     */
    public String generarToken(Usuario usuario) {
        Date ahora = new Date();
        Date caducidad = new Date(ahora.getTime() + caducidadMs);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuer(emisor)
                .issuedAt(ahora)
                .expiration(caducidad)
                .claim("id", usuario.getId())
                .claim("roles", List.of(usuario.getRol()))
                .claim("nombre", usuario.getNombre())
                // Se fija HS256 de forma explícita: con una clave de 64 bytes jjwt
                // elegiría HS512 y el decodificador (configurado para HS256) rechazaría el token.
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public SecretKey getClave() {
        return clave;
    }

    public long getCaducidadMs() {
        return caducidadMs;
    }

    public String getEmisor() {
        return emisor;
    }
}
