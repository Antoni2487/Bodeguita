package io.bootify.my_tiendita.detalle_pedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetallePedidoDTO {

    private Long id;

    @NotNull
    @Min(1)
    private Integer cantidad;

    // ID del ProductoBodega (No el producto global)
    @NotNull
    private Long productoBodegaId;

    // Campos de lectura (opcionales para respuesta)
    private BigDecimal subtotal;
    private String productoNombre;
    private BigDecimal precioUnitario;
}