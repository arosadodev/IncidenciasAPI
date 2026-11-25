package ovh.emserv.incidenciasapi.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ovh.emserv.incidenciasapi.DTO.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respuesta cuando el usuario ESTÁ autenticado pero su rol no le permite la
 * operación: HTTP 403 Forbidden.
 *
 * Ejemplos: un ROLE_USER que hace DELETE /incidencias/1 o que llama a /usuarios.
 * El código de la regla está en SecurityConfig (rutas) y en los @PreAuthorize
 * de los controladores.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                "No tienes permisos para realizar esta operación"));
    }
}
