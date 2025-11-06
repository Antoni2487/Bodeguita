package io.bootify.my_tiendita.bodega;

import io.bootify.my_tiendita.pedido.Pedido;
import io.bootify.my_tiendita.producto.Producto;
import io.bootify.my_tiendita.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
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

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(nullable = false, columnDefinition = "tinyint", length = 1)
    private Boolean activo;

    @OneToMany(mappedBy = "bodegas")
    private Set<Usuario> usuario = new HashSet<>();

    @OneToMany(mappedBy = "bodega")
    private Set<Producto> productos = new HashSet<>();

    @OneToMany(mappedBy = "bodega")
    private Set<Pedido> pedidos = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}
