package io.bootify.my_tiendita.pedido;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.detalle_pedido.DetallePedido;
import io.bootify.my_tiendita.model.EstadoPedido;
import io.bootify.my_tiendita.pago.BodegaMetodoPago;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.venta.Venta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalTime; // ✅ IMPORTANTE: Necesario para el campo fechaPedido
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Pedidos")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoPedido;

    // ✅ ESTE ES EL CAMPO QUE TE FALTA Y CAUSA EL ERROR
    @Column(nullable = false)
    private LocalTime fechaPedido; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal costoDelivery; 

    @Column(length = 255)
    private String direccionEntrega;

    @Column(length = 20)
    private String telefonoContacto;

    @Column
    private Double latitudEntrega;

    @Column
    private Double longitudEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_id", nullable = false)
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_metodo_pago_id")
    private BodegaMetodoPago metodoPago;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DetallePedido> detalles = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
    
    public void addDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }
}