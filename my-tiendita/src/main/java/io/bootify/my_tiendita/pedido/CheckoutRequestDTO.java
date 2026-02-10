package io.bootify.my_tiendita.pedido;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    @NotNull
    private Long bodegaId;

    @NotNull
    private Double latitud;
    @NotNull
    private Double longitud;

    private List<ItemCarritoDTO> productos;

    @Data
    public static class ItemCarritoDTO {
        private Long productoBodegaId;
        private Integer cantidad;
    }
}