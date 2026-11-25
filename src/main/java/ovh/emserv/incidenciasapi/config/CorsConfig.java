package ovh.emserv.incidenciasapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite llamar a la API desde el frontend en desarrollo, aunque se abra
 * desde otro origen (por ejemplo un Live Server en el puerto 5500 o file://,
 * que envía Origin: null).
 *
 * En producción conviene limitar los orígenes permitidos.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
