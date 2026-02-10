package io.bootify.my_tiendita.categoria;

import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(final CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<CategoriaDTO> findAll() {
        final List<Categoria> categorias = categoriaRepository.findAll(Sort.by("id"));
        return categorias.stream()
                .map(categoria -> mapToDTO(categoria, new CategoriaDTO()))
                .toList();
    }

    public CategoriaDTO get(final Long id) {
        return categoriaRepository.findById(id)
                .map(categoria -> mapToDTO(categoria, new CategoriaDTO()))
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
    }

    public Long create(final CategoriaDTO categoriaDTO) {
        final Categoria categoria = new Categoria();
        mapToEntity(categoriaDTO, categoria);
        return categoriaRepository.save(categoria).getId();
    }

    public void update(final Long id, final CategoriaDTO categoriaDTO) {
        final Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        mapToEntity(categoriaDTO, categoria);
        categoriaRepository.save(categoria);
    }

    public void delete(final Long id) {
        final Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        
        categoriaRepository.delete(categoria);
    }

    private CategoriaDTO mapToDTO(final Categoria categoria, final CategoriaDTO categoriaDTO) {
        categoriaDTO.setId(categoria.getId());
        categoriaDTO.setNombre(categoria.getNombre());
        categoriaDTO.setDescripcion(categoria.getDescripcion());
        return categoriaDTO;
    }

    private Categoria mapToEntity(final CategoriaDTO categoriaDTO, final Categoria categoria) {
        categoria.setNombre(categoriaDTO.getNombre());
        categoria.setDescripcion(categoriaDTO.getDescripcion());
        return categoria;
    }

    // Método existente para dropdowns
    public Map<Long, String> getCategoriaValues() {
        return categoriaRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Categoria::getId, Categoria::getNombre));
    }

}