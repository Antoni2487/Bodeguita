package io.bootify.my_tiendita.inventario;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventarioDTO {

    private Long id;
    private Long productoBodegaId;
    private Long tipoMovimientoId;
    private Integer cantidad;
    private String motivo;
    private Long referenciaId;
    private Long usuarioId;
    private LocalDateTime fecha;

    // Campos de solo lectura para el front
    private String productoNombre;
    private String categoriaNombre;
    private String tipoMovimientoNombre;
    private String usuarioNombre;
}
