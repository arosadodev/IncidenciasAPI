package ovh.emserv.incidenciasapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ovh.emserv.incidenciasapi.DTO.CambioEstadoRequest;
import ovh.emserv.incidenciasapi.DTO.IncidenciaRequest;
import ovh.emserv.incidenciasapi.DTO.IncidenciaResponse;
import ovh.emserv.incidenciasapi.service.IncidenciaService;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST de las incidencias.
 *
 * Los permisos por rol están en SecurityConfig; la propiedad de cada incidencia
 * se comprueba en IncidenciaService con el Authentication del token.
 */
@RestController
@RequestMapping("/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    /**
     * GET /incidencias
     * ROLE_USER recibe solo las suyas; TECNICO y ADMIN reciben todas.
     */
    @GetMapping
    public List<IncidenciaResponse> listar(Authentication autenticacion) {
        return incidenciaService.listar(autenticacion);
    }

    /**
     * GET /incidencias/{id}
     * Si un ROLE_USER pide una incidencia ajena, el servicio lanza 403.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidenciaResponse> obtener(@PathVariable Long id, Authentication autenticacion) {
        return incidenciaService.obtener(id, autenticacion)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /incidencias
     * El autor NO se recibe del cliente: se obtiene de la autenticación.
     */
    @PostMapping
    public ResponseEntity<IncidenciaResponse> crear(@Valid @RequestBody IncidenciaRequest datos,
                                                    Authentication autenticacion) {
        IncidenciaResponse creada = incidenciaService.crear(datos, autenticacion);
        return ResponseEntity.created(URI.create("/incidencias/" + creada.id())).body(creada);
    }

    /**
     * PATCH /incidencias/{id}/estado - solo TECNICO y ADMIN (ver SecurityConfig).
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<IncidenciaResponse> cambiarEstado(@PathVariable Long id,
                                                            @Valid @RequestBody CambioEstadoRequest datos) {
        return incidenciaService.cambiarEstado(id, datos.estado())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * DELETE /incidencias/{id} - solo ADMIN (ver SecurityConfig).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return incidenciaService.eliminar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
