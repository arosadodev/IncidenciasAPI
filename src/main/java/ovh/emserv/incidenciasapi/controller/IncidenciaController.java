package ovh.emserv.incidenciasapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ovh.emserv.incidenciasapi.DTO.CambioEstadoRequest;
import ovh.emserv.incidenciasapi.DTO.IncidenciaRequest;
import ovh.emserv.incidenciasapi.DTO.IncidenciaResponse;
import ovh.emserv.incidenciasapi.service.IncidenciaService;

import java.net.URI;
import java.util.List;

/**
 * Endpoints de incidencias.
 *
 * El controlador solo se ocupa de HTTP: recibe el Authentication que Spring
 * Security construye a partir del JWT y delega en el servicio, que es quien
 * comprueba la propiedad de cada incidencia.
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
     * Si un ROLE_USER pide una incidencia ajena, el servicio responde 403.
     */
    @GetMapping("/{id}")
    public IncidenciaResponse obtener(@PathVariable Long id, Authentication autenticacion) {
        return incidenciaService.obtener(id, autenticacion);
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

    /** PATCH /incidencias/{id}/estado - solo TECNICO y ADMIN. */
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMIN')")
    @PatchMapping("/{id}/estado")
    public IncidenciaResponse cambiarEstado(@PathVariable Long id,
                                            @Valid @RequestBody CambioEstadoRequest datos,
                                            Authentication autenticacion) {
        return incidenciaService.cambiarEstado(id, datos.estado(), autenticacion);
    }

    /** DELETE /incidencias/{id} - solo ADMIN. */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication autenticacion) {
        incidenciaService.eliminar(id, autenticacion);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
