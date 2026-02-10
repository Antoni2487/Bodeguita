package io.bootify.my_tiendita.producto_bodega;
import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;



@Entity
@Table(name = "Producto_Bodega", uniqueConstraints = {
    @UniqueConstraint(name = "uk_producto_bodega", columnNames = {"producto_id", "bodega_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ProductoBodega {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_id", nullable = false)
    private Bodega bodega;


    @Column(name = "precio_bodeguero", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBodeguero;


    @Column(nullable = false)
    private Integer stock;

  
    @Column(nullable = false, columnDefinition = "tinyint", length = 1)
    private Boolean activo;

 
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}