package io.bootify.my_tiendita.tipo_movimiento;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TipoMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre; 

    @Column(nullable = false)
    private String naturaleza; // "ENTRADA" o "SALIDA"
}
