package io.bootify.my_tiendita.producto;

import io.bootify.my_tiendita.categoria.Categoria;
import io.bootify.my_tiendita.categoria.CategoriaRepository;
import io.bootify.my_tiendita.subcategoria.Subcategoria;
import io.bootify.my_tiendita.subcategoria.SubcategoriaRepository;
import io.bootify.my_tiendita.events.BeforeDeleteProducto;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final ApplicationEventPublisher publisher;

    @Value("${upload.path:src/main/resources/static/uploads}")
    private String uploadPath;

    public ProductoService(final ProductoRepository productoRepository,
            final CategoriaRepository categoriaRepository,
            final SubcategoriaRepository subcategoriaRepository,
            final ApplicationEventPublisher publisher) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.subcategoriaRepository = subcategoriaRepository;
        this.publisher = publisher;
    }

    public List<ProductoDTO> findAll() {
        final List<Producto> productoes = productoRepository.findAll(Sort.by("id"));
        return productoes.stream()
                .map(producto -> mapToDTO(producto, new ProductoDTO()))
                .toList();
    }

    public ProductoDTO get(final Long id) {
        return productoRepository.findById(id)
                .map(producto -> mapToDTO(producto, new ProductoDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ProductoDTO productoDTO) {
        final Producto producto = new Producto();
        mapToEntity(productoDTO, producto);
        return productoRepository.save(producto).getId();
    }

    public void update(final Long id, final ProductoDTO productoDTO) {
        final Producto producto = productoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        
        // Guardar la imagen anterior para eliminarla después si cambió
        final String imagenAnterior = producto.getImagen();
        
        // Mapear los nuevos datos
        mapToEntity(productoDTO, producto);
        
        // Si la imagen cambió, eliminar la anterior del servidor
        final String imagenNueva = productoDTO.getImagen();
        if (imagenAnterior != null && !imagenAnterior.isEmpty() 
                && !imagenAnterior.equals(imagenNueva)) {
            deleteImageFile(imagenAnterior);
            log.info("Imagen anterior eliminada: {}", imagenAnterior);
        }
        
        productoRepository.save(producto);
    }

    public void delete(final Long id) {
        final Producto producto = productoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        
        // Eliminar la imagen del servidor antes de borrar el producto
        if (producto.getImagen() != null && !producto.getImagen().isEmpty()) {
            deleteImageFile(producto.getImagen());
            log.info("Imagen del producto eliminada: {}", producto.getImagen());
        }
        
        publisher.publishEvent(new BeforeDeleteProducto(id));
        productoRepository.delete(producto);
    }

    /**
     * Elimina un archivo de imagen del servidor
     * @param imageUrl URL de la imagen (ej: "/uploads/productos/uuid.jpg")
     */
    private void deleteImageFile(final String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
        
            String relativePath = imageUrl;
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            // Construir el path completo del archivo
            Path filePath = Paths.get(uploadPath).getParent().resolve(relativePath);

            // Verificar que el archivo existe y eliminarlo
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Archivo eliminado exitosamente: {}", filePath);
            } else {
                log.warn("Archivo no encontrado para eliminar: {}", filePath);
            }

        } catch (IOException e) {
            log.error("Error al eliminar archivo de imagen: {}", imageUrl, e);
        } catch (Exception e) {
            log.error("Error inesperado al eliminar imagen: {}", imageUrl, e);
        }
    }

    private ProductoDTO mapToDTO(final Producto producto, final ProductoDTO productoDTO) {
        productoDTO.setId(producto.getId());
        productoDTO.setNombre(producto.getNombre());
        productoDTO.setDescripcion(producto.getDescripcion());
        productoDTO.setPrecioSugerido(producto.getPrecioSugerido());
        productoDTO.setImagen(producto.getImagen());
        productoDTO.setActivo(producto.getActivo());
        productoDTO.setCategoria(producto.getCategoria() == null ? null : producto.getCategoria().getId());
        productoDTO.setSubcategoria(producto.getSubcategoria() == null ? null : producto.getSubcategoria().getId());
        return productoDTO;
    }

    private Producto mapToEntity(final ProductoDTO productoDTO, final Producto producto) {
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecioSugerido(productoDTO.getPrecioSugerido());
        producto.setImagen(productoDTO.getImagen());
        producto.setActivo(productoDTO.getActivo());

        
        final Categoria categoria = productoDTO.getCategoria() == null ? null : categoriaRepository.findById(productoDTO.getCategoria())
                .orElseThrow(() -> new NotFoundException("categoria not found"));
        producto.setCategoria(categoria);
        
        // Validación de Subcategoría
        if (productoDTO.getSubcategoria() != null) {
            final Subcategoria subcategoria = subcategoriaRepository.findById(productoDTO.getSubcategoria())
                    .orElseThrow(() -> new NotFoundException("subcategoria not found"));
            
            // Validar que la subcategoría pertenece a la categoría seleccionada
            if (categoria != null && !subcategoria.getCategoria().getId().equals(categoria.getId())) {
                throw new IllegalArgumentException("La subcategoría seleccionada no pertenece a la categoría elegida");
            }
            
            producto.setSubcategoria(subcategoria);
        } else {
            producto.setSubcategoria(null);
        }
        
        return producto;
    }

    public Map<Long, String> getProductoValues() {
        return productoRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Producto::getId, Producto::getNombre));
    }

    public long countAll() { 
        return productoRepository.count();
    }
}