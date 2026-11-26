package ovh.emserv.incidenciasapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
Incidencia del centro: una avería en un aula, un problema con un equipo informático, etc.
El autor es siempre el usuario autenticado que la crea.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidencias")
public class Incidencia {

    // Clave primaria de la entidad; JPA la identifica con @Id.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    // Los estados se guardan como texto (ABIERTA, EN_PROCESO o RESUELTA).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIncidencia estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Relación persistente de JPA con el autor de la incidencia.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    // Se ejecuta automáticamente antes de insertar una incidencia nueva.
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
