package io.bootify.my_tiendita.detalle_pedido;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class DetallePedidoDTO {

    private Long id;

    @NotNull
    private Integer cantidad;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "80.08")
    private BigDecimal subtotal;

    private Long pedido;

    private Long producto;

}
