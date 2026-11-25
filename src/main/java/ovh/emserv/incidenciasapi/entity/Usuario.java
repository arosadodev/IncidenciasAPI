package ovh.emserv.incidenciasapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Usuario de la aplicación.
 *
 * Implementa UserDetails porque es la clase que Spring Security usa durante el login
 * (AuthenticationManager -> CustomUserDetailsService -> este objeto) y también para
 * saber si la cuenta está activa.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 60)
    private String nombre;

    /** Contraseña cifrada con BCrypt; nunca se guarda en claro. */
    @Column(nullable = false)
    private String password;

    /**
     * Rol del usuario. Solo se admiten ROLE_USER, ROLE_TECNICO y ROLE_ADMIN
     * (la validación está en UsuarioService, no en la entidad).
     */
    @Column(nullable = false, length = 20)
    private String rol;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Se ejecuta automáticamente antes de insertar un usuario nuevo. */
    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.activo == null) {
            this.activo = true;
        }
    }

    /**
     * Spring Security consulta este método para saber los permisos del usuario.
     * El rol ya se guarda con el prefijo ROLE_, así que no hay que añadirlo.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol));
    }

    /** El nombre de usuario para Spring Security es el email. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(activo);
    }
}
