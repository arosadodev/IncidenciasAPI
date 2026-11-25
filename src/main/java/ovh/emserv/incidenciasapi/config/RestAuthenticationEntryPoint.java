package ovh.emserv.incidenciasapi.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ovh.emserv.incidenciasapi.DTO.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respuesta cuando la petición NO está autenticada: HTTP 401 Unauthorized.
 *
 * Se produce cuando falta el token, cuando está mal firmado, cuando ha caducado
 * o cuando el emisor no es el esperado. Lo lanza BearerTokenAuthenticationFilter /
 * el ExceptionTranslationFilter de Spring Security, no un controlador.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                "No autenticado: envía un token JWT válido en la cabecera Authorization"));
    }
}
