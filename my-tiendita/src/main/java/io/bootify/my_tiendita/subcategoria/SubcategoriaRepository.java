package io.bootify.my_tiendita.subcategoria;

import io.bootify.my_tiendita.categoria.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

    // Encontrar todas las subcategorías de una categoría específica
    List<Subcategoria> findByCategoria(Categoria categoria);
    
    // Encontrar todas las subcategorías por ID de categoría
    List<Subcategoria> findByCategoriaId(Long categoriaId);
    
    // Verificar si existe una subcategoría con ese nombre en una categoría
    boolean existsByNombreAndCategoriaId(String nombre, Long categoriaId);

}