package io.bootify.my_tiendita.producto_bodega;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST Controller para ProductoBodega
 * 
 * Gestiona los endpoints para que los bodegueros administren
 * los productos de su bodega (precio, stock, disponibilidad).
 */
@RestController
@RequestMapping(value = "/api/producto-bodega", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ProductoBodega", description = "API para gestión de productos en bodegas")
public class ProductoBodegaResource {

    private final ProductoBodegaService productoBodegaService;

    public ProductoBodegaResource(final ProductoBodegaService productoBodegaService) {
        this.productoBodegaService = productoBodegaService;
    }

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los productos de una bodega específica
     * GET /api/producto-bodega/bodega/{bodegaId}
     */
    @GetMapping("/bodega/{bodegaId}")
    @Operation(summary = "Listar productos de una bodega", 
               description = "Obtiene todos los productos asignados a una bodega específica con su precio y stock")
    public ResponseEntity<List<ProductoBodegaDTO>> getAllByBodega(
            @Parameter(description = "ID de la bodega", required = true)
            @PathVariable(name = "bodegaId") final Long bodegaId) {
        return ResponseEntity.ok(productoBodegaService.findAllByBodega(bodegaId));
    }

    /**
     * Obtener un producto específico de una bodega por ID
     * GET /api/producto-bodega/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto de bodega", 
               description = "Obtiene la información de un producto específico en una bodega")
    public ResponseEntity<ProductoBodegaDTO> getProductoBodega(
            @Parameter(description = "ID del ProductoBodega", required = true)
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(productoBodegaService.get(id));
    }

    /**
     * Agregar un producto del catálogo a una bodega
     * POST /api/producto-bodega
     */
    @PostMapping
    @ApiResponse(responseCode = "201", description = "Producto agregado exitosamente")
    @Operation(summary = "Agregar producto a bodega", 
               description = "Agrega un producto del catálogo a una bodega con precio y stock inicial")
    public ResponseEntity<Long> createProductoBodega(
            @RequestBody @Valid final ProductoBodegaDTO productoBodegaDTO) {
        final Long createdId = productoBodegaService.create(productoBodegaDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    /**
     * Actualizar precio, stock o estado de un producto en una bodega
     * PUT /api/producto-bodega/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto de bodega", 
               description = "Actualiza el precio, stock o disponibilidad de un producto en una bodega")
    public ResponseEntity<Long> updateProductoBodega(
            @Parameter(description = "ID del ProductoBodega", required = true)
            @PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ProductoBodegaDTO productoBodegaDTO) {
        productoBodegaService.update(id, productoBodegaDTO);
        return ResponseEntity.ok(id);
    }

    /**
     * Eliminar un producto de una bodega
     * DELETE /api/producto-bodega/{id}
     */
    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Producto eliminado de la bodega")
    @Operation(summary = "Eliminar producto de bodega", 
               description = "Elimina la asignación de un producto a una bodega (no elimina el producto del catálogo)")
    public ResponseEntity<Void> deleteProductoBodega(
            @Parameter(description = "ID del ProductoBodega", required = true)
            @PathVariable(name = "id") final Long id) {
        productoBodegaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== CONSULTAS ESPECIALES ====================

    /**
     * Obtener un producto específico de una bodega (por IDs de producto y bodega)
     * GET /api/producto-bodega/producto/{productoId}/bodega/{bodegaId}
     */
    @GetMapping("/producto/{productoId}/bodega/{bodegaId}")
    @Operation(summary = "Buscar producto en bodega", 
               description = "Busca un producto específico dentro de una bodega")
    public ResponseEntity<ProductoBodegaDTO> getByProductoAndBodega(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable(name = "productoId") final Long productoId,
            @Parameter(description = "ID de la bodega", required = true)
            @PathVariable(name = "bodegaId") final Long bodegaId) {
        return ResponseEntity.ok(productoBodegaService.getByProductoAndBodega(productoId, bodegaId));
    }

    /**
     * Buscar productos por nombre en una bodega
     * GET /api/producto-bodega/bodega/{bodegaId}/buscar?nombre=coca
     */
    @GetMapping("/bodega/{bodegaId}/buscar")
    @Operation(summary = "Buscar productos por nombre", 
               description = "Busca productos en una bodega por nombre (búsqueda parcial)")
    public ResponseEntity<List<ProductoBodegaDTO>> searchByNombre(
            @Parameter(description = "ID de la bodega", required = true)
            @PathVariable(name = "bodegaId") final Long bodegaId,
            @Parameter(description = "Nombre o parte del nombre del producto", required = true)
            @RequestParam(name = "nombre") final String nombre) {
        return ResponseEntity.ok(productoBodegaService.searchByNombre(bodegaId, nombre));
    }

    /**
     * Filtrar productos por categoría en una bodega
     * GET /api/producto-bodega/bodega/{bodegaId}/categoria/{categoriaId}
     */
    @GetMapping("/bodega/{bodegaId}/categoria/{categoriaId}")
    @Operation(summary = "Filtrar productos por categoría", 
               description = "Obtiene todos los productos de una categoría específica en una bodega")
    public ResponseEntity<List<ProductoBodegaDTO>> getByCategoria(
            @Parameter(description = "ID de la bodega", required = true)
            @PathVariable(name = "bodegaId") final Long bodegaId,
            @Parameter(description = "ID de la categoría", required = true)
            @PathVariable(name = "categoriaId") final Long categoriaId) {
        return ResponseEntity.ok(productoBodegaService.findByCategoria(bodegaId, categoriaId));
    }

    /**
     * Obtener solo productos activos de una bodega
     * GET /api/producto-bodega/bodega/{bodegaId}/activos
     */
    @GetMapping("/bodega/{bodegaId}/activos")
    @Operation(summary = "Listar productos activos", 
               description = "Obtiene solo los productos activos/disponibles de una bodega")
    public ResponseEntity<List<ProductoBodegaDTO>> getActivosByBodega(
            @Parameter(description = "ID de la bodega", required = true)
            @PathVariable(name = "bodegaId") final Long bodegaId) {
        return ResponseEntity.ok(productoBodegaService.findActivosByBodega(bodegaId));
    }

}