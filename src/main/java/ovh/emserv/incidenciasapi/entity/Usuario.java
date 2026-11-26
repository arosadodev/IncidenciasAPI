package ovh.emserv.incidenciasapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/*
- Registrar qué usuario añade cada incidencia
- Añadir repositorio y controlador para Usuario (sólo registrar)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    // Clave primaria de la entidad; JPA la identifica con @Id.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dato email de tipo String; JPA lo mapea a una columna cuando pertenece a una entidad.
    @Column(unique = true, nullable = false)
    private String email;

    // Atributo nombre de tipo String; en una entidad JPA se mapea a una columna salvo configuración distinta.
    @Column(nullable = false)
    private String nombre;

    // Contraseña del usuario; la capa de autenticación almacena su valor cifrado con BCrypt.
    @Column(nullable = false)
    private String password;

    // Rol del usuario: ROLE_USER, ROLE_TECNICO o ROLE_ADMIN.
    @Column(nullable = false)
    private String rol;

    // Atributo activo de tipo Boolean; lo usa isEnabled() para permitir o no el login.
    @Column(nullable = false)
    private Boolean activo;

    /**
     * Convierte el valor de "rol" en el permiso que entiende Spring Security.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol));
    }

    /**
     * El nombre de usuario para Spring Security es el email.
     */
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
