package io.bootify.my_tiendita.subcategoria;

import io.bootify.my_tiendita.categoria.Categoria;
import io.bootify.my_tiendita.categoria.CategoriaRepository;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;

    public SubcategoriaService(final SubcategoriaRepository subcategoriaRepository,
            final CategoriaRepository categoriaRepository) {
        this.subcategoriaRepository = subcategoriaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<SubcategoriaDTO> findAll() {
        final List<Subcategoria> subcategorias = subcategoriaRepository.findAll(Sort.by("id"));
        return subcategorias.stream()
                .map(subcategoria -> mapToDTO(subcategoria, new SubcategoriaDTO()))
                .toList();
    }

    public SubcategoriaDTO get(final Long id) {
        return subcategoriaRepository.findById(id)
                .map(subcategoria -> mapToDTO(subcategoria, new SubcategoriaDTO()))
                .orElseThrow(() -> new NotFoundException("Subcategoría no encontrada"));
    }

    public Long create(final SubcategoriaDTO subcategoriaDTO) {
        final Subcategoria subcategoria = new Subcategoria();
        mapToEntity(subcategoriaDTO, subcategoria);
        return subcategoriaRepository.save(subcategoria).getId();
    }

    public void update(final Long id, final SubcategoriaDTO subcategoriaDTO) {
        final Subcategoria subcategoria = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subcategoría no encontrada"));
        mapToEntity(subcategoriaDTO, subcategoria);
        subcategoriaRepository.save(subcategoria);
    }

    public void delete(final Long id) {
        subcategoriaRepository.deleteById(id);
    }

    private SubcategoriaDTO mapToDTO(final Subcategoria subcategoria,
            final SubcategoriaDTO subcategoriaDTO) {
        subcategoriaDTO.setId(subcategoria.getId());
        subcategoriaDTO.setNombre(subcategoria.getNombre());
        subcategoriaDTO.setDescripcion(subcategoria.getDescripcion());
        subcategoriaDTO.setCategoriaId(subcategoria.getCategoria() == null ? null : 
                subcategoria.getCategoria().getId());
        return subcategoriaDTO;
    }

    private Subcategoria mapToEntity(final SubcategoriaDTO subcategoriaDTO,
            final Subcategoria subcategoria) {
        subcategoria.setNombre(subcategoriaDTO.getNombre());
        subcategoria.setDescripcion(subcategoriaDTO.getDescripcion());
        
        final Categoria categoria = subcategoriaDTO.getCategoriaId() == null ? null :
                categoriaRepository.findById(subcategoriaDTO.getCategoriaId())
                        .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        subcategoria.setCategoria(categoria);
        
        return subcategoria;
    }

    // Obtener todas las subcategorías de una categoría específica (devuelve DTO)
    public List<SubcategoriaDTO> findByCategoriaId(final Long categoriaId) {
        return subcategoriaRepository.findByCategoriaId(categoriaId)
                .stream()
                .map(subcategoria -> mapToDTO(subcategoria, new SubcategoriaDTO()))
                .toList();
    }

    // Obtener subcategorías como Map para dropdowns
    public Map<Long, String> getSubcategoriaValues() {
        return subcategoriaRepository.findAll(Sort.by("nombre"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Subcategoria::getId, Subcategoria::getNombre));
    }

    // Obtener subcategorías de una categoría como Map para dropdowns
    public Map<Long, String> getSubcategoriaValuesByCategoriaId(final Long categoriaId) {
        return subcategoriaRepository.findByCategoriaId(categoriaId)
                .stream()
                .collect(CustomCollectors.toSortedMap(Subcategoria::getId, Subcategoria::getNombre));
    }

    // Validar que la subcategoría pertenece a la categoría
    public boolean subcategoriaPertenecesACategoria(final Long subcategoriaId, final Long categoriaId) {
        if (subcategoriaId == null || categoriaId == null) {
            return false;
        }
        
        Subcategoria subcategoria = subcategoriaRepository.findById(subcategoriaId)
                .orElseThrow(() -> new NotFoundException("Subcategoría no encontrada"));
        
        return subcategoria.getCategoria().getId().equals(categoriaId);
    }

}