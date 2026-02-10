package io.bootify.my_tiendita.usuario;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.pedido.Pedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 9)
    private String telefono;

    @Column(length = 100)
    private String direccion;

    @Column
    private Double latitud;

    @Column
    private Double longitud;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(unique = true, length = 11)
    private String numeroDocumento;

    /** ===================== ROLES (N:N) ===================== */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    /** ===================== BODEGAS (N:N) ===================== */
    @ManyToMany
    @JoinTable(
        name = "usuario_bodega",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "bodega_id")
    )
    private Set<Bodega> bodegas = new HashSet<>();

    /** ===================== PEDIDOS (1:N) ===================== */
    @OneToMany(mappedBy = "usuario")
    private Set<Pedido> pedidos = new HashSet<>();

    /** ===================== AUDITORÍA ===================== */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}
