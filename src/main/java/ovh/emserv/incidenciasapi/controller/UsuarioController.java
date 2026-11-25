package ovh.emserv.incidenciasapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ovh.emserv.incidenciasapi.DTO.CambioRolRequest;
import ovh.emserv.incidenciasapi.DTO.UsuarioResponse;
import ovh.emserv.incidenciasapi.service.UsuarioService;

import java.util.List;

/**
 * Administración de usuarios.
 *
 * Todo el controlador exige ROLE_ADMIN: aunque el frontend oculte el panel a
 * otros roles, cualquiera que llame directamente a la API recibe 403.
 */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** GET /usuarios - listado de usuarios (sin contraseñas). */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /**
     * PATCH /usuarios/{id}/rol
     * Cuerpo: {"rol": "ROLE_TECNICO"}. Si el rol no está en la lista blanca,
     * el servicio devuelve 400; si el usuario no existe, 404.
     */
    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambioRolRequest datos) {
        return usuarioService.cambiarRol(id, datos.rol());
    }
}
