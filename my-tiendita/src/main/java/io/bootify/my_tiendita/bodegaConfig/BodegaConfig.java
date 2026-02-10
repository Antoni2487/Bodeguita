package io.bootify.my_tiendita.bodegaConfig;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

import io.bootify.my_tiendita.bodega.Bodega;

@Entity
@Getter
@Setter
public class BodegaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "bodega_id", nullable = false)
    private Bodega bodega;

    // REGLAS DE DELIVERY
    private Boolean realizaDelivery; 
    
    @Column(precision = 10, scale = 2)
    private BigDecimal radioMaximoKm; 
    
    @Column(precision = 10, scale = 2)
    private BigDecimal precioPorKm;   
    
    @Column(precision = 10, scale = 2)
    private BigDecimal pedidoMinimoDelivery; 

}