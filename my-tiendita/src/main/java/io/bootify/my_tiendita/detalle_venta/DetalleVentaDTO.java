package io.bootify.my_tiendita.detalle_venta;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class DetalleVentaDTO {
    
    @NotNull
    private Long productoBodegaId;
    
    @NotNull
    @Min(1)
    private Integer cantidad;

    private String nombreProducto;
    private String imagenProductoUrl;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}