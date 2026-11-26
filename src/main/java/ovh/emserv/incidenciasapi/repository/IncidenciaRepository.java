package ovh.emserv.incidenciasapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ovh.emserv.incidenciasapi.entity.Incidencia;

import java.util.List;

/**
 * Repositorio Spring Data JPA para las incidencias.
 */
@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    // Todas las incidencias (lo usan TECNICO y ADMIN).
    List<Incidencia> findAllByOrderByFechaCreacionDesc();

    // Solo las incidencias de un autor concreto (lo usa ROLE_USER).
    List<Incidencia> findByAutorEmailIgnoreCaseOrderByFechaCreacionDesc(String email);
}
