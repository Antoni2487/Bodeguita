package io.bootify.my_tiendita.pedido;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PedidoDTO {

    private Long id;

    @NotNull
    @Schema(type = "string", example = "18:30")
    private LocalTime fechaPedido;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "32.08")
    private BigDecimal total;

    @NotNull
    @Size(max = 255)
    private String direccionEntrega;

    private Long usuario;

    private Long bodega;

    @PedidoVentaUnique
    private Long venta;

}
