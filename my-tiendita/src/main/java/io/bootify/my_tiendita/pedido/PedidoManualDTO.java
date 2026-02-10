package io.bootify.my_tiendita.pedido;

import lombok.Data;
import java.util.List;

@Data
public class PedidoManualDTO {
    private Long bodegaId;
    private Long usuarioId; 
    private String direccionEntrega; 
    private List<ItemManualDTO> productos;

    @Data
    public static class ItemManualDTO {
        private Long productoBodegaId;
        private Integer cantidad;
    }
}