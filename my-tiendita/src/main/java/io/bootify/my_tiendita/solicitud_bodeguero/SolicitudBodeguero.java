package io.bootify.my_tiendita.solicitud_bodeguero;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "Solicitudes_Bodeguero")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class SolicitudBodeguero {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- DATOS DEL SOLICITANTE (Futuro Usuario) ---
    @Column(nullable = false, length = 100)
    private String nombreSolicitante; // Ej: Juan Pérez

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 15)
    private String telefono;

    // --- DATOS DE LA BODEGA (Futura Bodega) ---
    @Column(nullable = false, length = 150)
    private String nombreBodega; // Ej: Bodega Don Pepe

    @Column(nullable = false, length = 255)
    private String direccionBodega; // Ej: Av. Siempre Viva 123

    @Column(length = 20)
    private String ruc; // Opcional: Para validar si es negocio formal

    // --- ESTADO DEL PROCESO ---
    @Column(nullable = false, length = 20)
    private String estado; // PENDIENTE, APROBADA, RECHAZADA

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}