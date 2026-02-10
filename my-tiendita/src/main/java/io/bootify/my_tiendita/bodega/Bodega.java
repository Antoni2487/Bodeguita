package io.bootify.my_tiendita.bodega;

import io.bootify.my_tiendita.bodegaConfig.BodegaConfig;
import io.bootify.my_tiendita.pago.BodegaMetodoPago;
import io.bootify.my_tiendita.pedido.Pedido;
import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.usuario.Usuario;
import jakarta.persistence.*; // Importa todo jakarta.persistence
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "Bodegas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Bodega {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(nullable = false, columnDefinition = "tinyint", length = 1)
    private Boolean activo;

    @Column(length = 15)
    private String telefono;

    @Column(length = 100)
    private String distrito;

    @Column(length = 100)
    private String horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

 
    @ManyToMany(mappedBy = "bodegas", fetch = FetchType.LAZY) 
    private Set<Usuario> bodegueros = new HashSet<>();

    @OneToMany(mappedBy = "bodega", fetch = FetchType.LAZY)
    private Set<ProductoBodega> productosBodega = new HashSet<>();

    @OneToMany(mappedBy = "bodega", fetch = FetchType.LAZY)
    private Set<Pedido> pedidos = new HashSet<>();

    @OneToOne(mappedBy = "bodega", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BodegaConfig configuracion;

    @OneToMany(mappedBy = "bodega", cascade = CascadeType.ALL)
    private List<BodegaMetodoPago> metodosPago;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}