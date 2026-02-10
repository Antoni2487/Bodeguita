package io.bootify.my_tiendita.bodegaConfig;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class BodegaConfigDTO {

    private Long id; // ID de la config (si existe)

    // --- 1. DATOS GENERALES (Vienen de la entidad Bodega) ---
    @NotBlank(message = "El nombre de la bodega es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    private String telefono;
    private String horario; 

   
    @NotNull(message = "Debes ubicar tu bodega en el mapa")
    private Double latitud;

    @NotNull(message = "Debes ubicar tu bodega en el mapa")
    private Double longitud;

    @NotNull
    private Boolean realizaDelivery;

    @DecimalMin("0.0")
    private BigDecimal radioMaximoKm;

    @DecimalMin("0.0")
    private BigDecimal precioPorKm;

    @DecimalMin("0.0")
    private BigDecimal pedidoMinimoDelivery;
    
    private Integer tiempoEntregaMinutos;
}