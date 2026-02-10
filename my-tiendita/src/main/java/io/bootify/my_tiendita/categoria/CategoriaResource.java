package io.bootify.my_tiendita.categoria;
import io.bootify.my_tiendita.subcategoria.SubcategoriaDTO;
import io.bootify.my_tiendita.subcategoria.SubcategoriaService;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping(value = "/api/categorias", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoriaResource {

    private final CategoriaService categoriaService;
    private final SubcategoriaService subcategoriaService;

    public CategoriaResource(final CategoriaService categoriaService,
            final SubcategoriaService subcategoriaService) {
        this.categoriaService = categoriaService;
        this.subcategoriaService = subcategoriaService;  
    }


    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> getAllCategorias() {
        return ResponseEntity.ok(categoriaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> getCategoria(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(categoriaService.get(id));
    }

    @PostMapping
    public ResponseEntity<Long> createCategoria(
            @RequestBody @Valid final CategoriaDTO categoriaDTO) {
        final Long createdId = categoriaService.create(categoriaDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateCategoria(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final CategoriaDTO categoriaDTO) {
        categoriaService.update(id, categoriaDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategoria(@PathVariable(name = "id") final Long id) {
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/subcategorias")
    public ResponseEntity<List<SubcategoriaDTO>> getSubcategoriasByCategoria(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(subcategoriaService.findByCategoriaId(id));
    }

}