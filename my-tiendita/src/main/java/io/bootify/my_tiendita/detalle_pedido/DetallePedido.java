package io.bootify.my_tiendita.detalle_pedido;

import io.bootify.my_tiendita.pedido.Pedido;
import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime; // ✅ Importar fechas

@Entity
@Table(name = "Detalle_Pedidos")
@EntityListeners(AuditingEntityListener.class) // ✅ Activa la auditoría automática
@Getter
@Setter
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_bodega_id", nullable = false)
    private ProductoBodega productoBodega;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // ✅ CAMPOS FALTANTES AGREGADOS
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}