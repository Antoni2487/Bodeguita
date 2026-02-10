package io.bootify.my_tiendita.producto_bodega;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoBodegaDTO {

    private Long id;

    
    @NotNull(message = "El producto es obligatorio")
    @Schema(description = "ID del producto", example = "1")
    private Long producto;

   
    @NotNull(message = "La bodega es obligatoria")
    @Schema(description = "ID de la bodega", example = "1")
    private Long bodega;

 
    @NotNull(message = "El precio es obligatorio")
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "2.50", description = "Precio de venta en esta bodega")
    private BigDecimal precioBodeguero;

    
    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Schema(description = "Cantidad disponible en stock", example = "100")
    private Integer stock;

    
    @NotNull(message = "El estado activo es obligatorio")
    @Schema(description = "¿Está activo en esta bodega?", example = "true")
    private Boolean activo;

    // ========== Campos adicionales para la respuesta (no se envían en request) ==========

    @Schema(description = "Nombre del producto", example = "Coca Cola 500ml", accessMode = Schema.AccessMode.READ_ONLY)
    private String productoNombre;

  
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "2.50", description = "Precio sugerido por la administración", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal precioSugerido;


    @Schema(description = "URL de la imagen del producto", example = "/uploads/productos/abc123.jpg", accessMode = Schema.AccessMode.READ_ONLY)
    private String productoImagen;

   
    @Schema(description = "Nombre de la bodega", example = "Bodega Norte", accessMode = Schema.AccessMode.READ_ONLY)
    private String bodegaNombre;

  
    @Schema(description = "ID de la categoría del producto", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long categoriaId;


    @Schema(description = "Nombre de la categoría", example = "Bebidas", accessMode = Schema.AccessMode.READ_ONLY)
    private String categoriaNombre;

}