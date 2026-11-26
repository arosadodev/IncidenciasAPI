package ovh.emserv.incidenciasapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ovh.emserv.incidenciasapi.DTO.CambioRolRequest;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.service.UsuarioService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controlador REST de administración de usuarios.
 * Todas sus rutas exigen ROLE_ADMIN en SecurityConfig.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    // Lista blanca de roles que la API acepta al cambiar permisos.
    private static final Set<String> ROLES_ADMITIDOS =
            Set.of("ROLE_USER", "ROLE_TECNICO", "ROLE_ADMIN");

    private final UsuarioService usuarioService;

    /**
     * GET /usuarios - listado de usuarios sin contraseñas.
     */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /**
     * PATCH /usuarios/{id}/rol
     * Cuerpo: {"rol": "ROLE_TECNICO"}. Un rol no admitido devuelve 400.
     */
    @PatchMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id,
                                        @Valid @RequestBody CambioRolRequest datos) {
        String rol = datos.rol().trim();

        if (!ROLES_ADMITIDOS.contains(rol)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            "Rol no válido. Valores admitidos: ROLE_USER, ROLE_TECNICO, ROLE_ADMIN"));
        }

        return usuarioService.cambiarRol(id, rol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
