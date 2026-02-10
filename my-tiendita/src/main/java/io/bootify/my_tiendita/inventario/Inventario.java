package io.bootify.my_tiendita.inventario;

import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.tipo_movimiento.TipoMovimiento;
import io.bootify.my_tiendita.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ProductoBodega productoBodega;

    @ManyToOne(optional = false)
    private TipoMovimiento tipoMovimiento;

    @Column(nullable = false)
    private Integer cantidad;

    private String motivo;

    private Long referenciaId;

    @ManyToOne
    private Usuario usuario;

    private LocalDateTime fecha;
}
