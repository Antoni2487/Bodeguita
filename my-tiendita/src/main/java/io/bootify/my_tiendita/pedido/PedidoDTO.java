package io.bootify.my_tiendita.pedido;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.bootify.my_tiendita.detalle_pedido.DetallePedidoDTO; // Importante
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List; // Importante
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoDTO {

    private Long id;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String codigoPedido;

    // Se genera en el backend, no es obligatorio que venga del front
    @Schema(type = "string", example = "2023-10-31T18:30:00Z")
    private OffsetDateTime fechaPedido;

    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // El back lo calcula
    private BigDecimal total;
    
    private String estado;

    @NotNull
    @Size(max = 255)
    private String direccionEntrega;
    
    @Size(max = 20)
    private String telefonoContacto;

    private Long usuario;
    
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String usuarioNombre;

    private Long bodega;

    private List<DetallePedidoDTO> detalles; 

    private Long venta; 
}