package ovh.emserv.incidenciasapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Incidencia del centro: por ejemplo una avería en un aula o un problema
 * con un equipo informático.
 *
 * El autor es siempre un Usuario ya autenticado. El estado inicial lo pone
 * el backend (ABIERTA), no el cliente.
 */
@Entity
@Table(name = "incidencias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIncidencia estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    /**
     * Autor de la incidencia. Se obtiene en el servicio a partir del usuario
     * autenticado (nunca desde el cuerpo de la petición).
     * FetchType.LAZY: el autor solo se consulta cuando se necesita (por ejemplo
     * al montar el DTO de respuesta dentro de la transacción).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
        if (this.estado == null) {
            this.estado = EstadoIncidencia.ABIERTA;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
