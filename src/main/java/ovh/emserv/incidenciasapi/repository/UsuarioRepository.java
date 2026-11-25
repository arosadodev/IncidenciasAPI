package ovh.emserv.incidenciasapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovh.emserv.incidenciasapi.entity.Usuario;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para los usuarios.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
