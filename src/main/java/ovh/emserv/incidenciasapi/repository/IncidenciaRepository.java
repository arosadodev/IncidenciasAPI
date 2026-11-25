package ovh.emserv.incidenciasapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovh.emserv.incidenciasapi.entity.Incidencia;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para las incidencias.
 *
 * Los métodos derivados permiten filtrar por el email del autor, que es la
 * clave que usa el reto de "solo mis incidencias".
 */
@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    /** Todas las incidencias (lo usan TECNICO y ADMIN). */
    List<Incidencia> findAllByOrderByFechaCreacionDesc();

    /** Solo las incidencias de un autor concreto (lo usa ROLE_USER). */
    List<Incidencia> findByAutorEmailIgnoreCaseOrderByFechaCreacionDesc(String email);

    Optional<Incidencia> findByIdAndAutorEmailIgnoreCase(Long id, String email);

    boolean existsByIdAndAutorEmailIgnoreCase(Long id, String email);
}
