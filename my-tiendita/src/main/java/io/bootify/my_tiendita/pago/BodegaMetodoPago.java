package io.bootify.my_tiendita.pago;

import io.bootify.my_tiendita.bodega.Bodega;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class BodegaMetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_id", nullable = false)
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_metodo_pago_id", nullable = false)
    private TipoMetodoPago tipoMetodoPago; 

   
    private String nombreTitular; 
    private String numeroTelefono; 
    
    @Column(length = 1000)
    private String imagenQrUrl; 
    
    private Boolean activo; 
}