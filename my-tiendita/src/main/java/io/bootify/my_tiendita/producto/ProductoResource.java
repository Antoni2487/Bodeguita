package io.bootify.my_tiendita.producto;

import io.bootify.my_tiendita.categoria.CategoriaService;
import io.bootify.my_tiendita.subcategoria.SubcategoriaService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/productos", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductoResource {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;

    public ProductoResource(final ProductoService productoService,
            final CategoriaService categoriaService,
            final SubcategoriaService subcategoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.subcategoriaService = subcategoriaService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> getAllProductos() {
        return ResponseEntity.ok(productoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> getProducto(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(productoService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createProducto(@RequestBody @Valid final ProductoDTO productoDTO) {
        final Long createdId = productoService.create(productoDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateProducto(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final ProductoDTO productoDTO) {
        productoService.update(id, productoDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteProducto(@PathVariable(name = "id") final Long id) {
        productoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoints útiles para formularios - obtener categorías para dropdown
    @GetMapping("/form-data/categorias")
    public ResponseEntity<Map<Long, String>> getCategoriasForForm() {
        return ResponseEntity.ok(categoriaService.getCategoriaValues());
    }

    // Obtener subcategorías filtradas por categoría para dropdown
    @GetMapping("/form-data/subcategorias/categoria/{categoriaId}")
    public ResponseEntity<Map<Long, String>> getSubcategoriasForForm(
            @PathVariable(name = "categoriaId") final Long categoriaId) {
        return ResponseEntity.ok(subcategoriaService.getSubcategoriaValuesByCategoriaId(categoriaId));
    }

}