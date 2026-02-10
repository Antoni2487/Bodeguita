package io.bootify.my_tiendita.venta;

import io.bootify.my_tiendita.detalle_venta.DetalleVenta;
import io.bootify.my_tiendita.model.MetodoEntrega;
import io.bootify.my_tiendita.pago.BodegaMetodoPago;
import io.bootify.my_tiendita.pago.TipoMetodoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoEntrega tipoEntrega;

    @Column(precision = 10, scale = 2)
    private BigDecimal costoDelivery;

    @Column
    private Double latitudEntrega;

    @Column
    private Double longitudEntrega;

    @Column
    private String direccionEntrega;

    // --- ✅ CAMPOS NUEVOS AGREGADOS ---
    
    @Column
    private String clienteNombre;

    @Column(nullable = false)
    private String estado; // 'COMPLETADA', 'ANULADA'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_metodo_pago_id")
    private TipoMetodoPago tipoMetodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_metodo_pago_id")
    private BodegaMetodoPago bodegaMetodoPago;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}