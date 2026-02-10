package io.bootify.my_tiendita.tienda;

import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.categoria.Categoria;
import io.bootify.my_tiendita.categoria.CategoriaRepository;
import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaDTO;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TiendaService {

    private final ProductoBodegaRepository productoBodegaRepository;
    private final BodegaRepository bodegaRepository;
    private final CategoriaRepository categoriaRepository;

    public TiendaService(ProductoBodegaRepository productoBodegaRepository,
                         BodegaRepository bodegaRepository,
                         CategoriaRepository categoriaRepository) {
        this.productoBodegaRepository = productoBodegaRepository;
        this.bodegaRepository = bodegaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Obtiene el catálogo completo disponible para una bodega
     */
    public List<ProductoBodegaDTO> obtenerCatalogo(Long bodegaId) {
        validarBodega(bodegaId);
        return productoBodegaRepository.findProductosDisponiblesPorBodega(bodegaId)
                .stream()
                .map(this::mapToTiendaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene productos filtrados por categoría
     */
    public List<ProductoBodegaDTO> obtenerPorCategoria(Long bodegaId, Long categoriaId) {
        validarBodega(bodegaId);
        return productoBodegaRepository.findProductosDisponiblesPorCategoria(bodegaId, categoriaId)
                .stream()
                .map(this::mapToTiendaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene detalle de un producto específico para la vista "Detalle de Producto"
     */
    public ProductoBodegaDTO obtenerDetalleProducto(Long productoBodegaId) {
        ProductoBodega pb = productoBodegaRepository.findById(productoBodegaId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        
        if (!pb.getActivo() || pb.getStock() <= 0) {
            throw new NotFoundException("Este producto ya no está disponible");
        }
        return mapToTiendaDTO(pb);
    }

    /**
     * Extrae las categorías que tienen productos disponibles en esa bodega
     * (Para pintar el menú de filtros sin categorías vacías)
     */
    public Map<Long, String> obtenerCategoriasConProductos(Long bodegaId) {
        List<ProductoBodega> productos = productoBodegaRepository.findProductosDisponiblesPorBodega(bodegaId);
        
        // Agrupamos y extraemos categorías únicas
        return productos.stream()
                .map(pb -> pb.getProducto().getCategoria())
                .distinct()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
    }

    private void validarBodega(Long bodegaId) {
        if (!bodegaRepository.existsById(bodegaId)) {
            throw new NotFoundException("La bodega seleccionada no existe");
        }
    }

    // Mapper simplificado para la Tienda
    private ProductoBodegaDTO mapToTiendaDTO(ProductoBodega entity) {
        ProductoBodegaDTO dto = new ProductoBodegaDTO();
        dto.setId(entity.getId());
        dto.setProducto(entity.getProducto().getId());
        dto.setProductoNombre(entity.getProducto().getNombre());
        dto.setProductoImagen(entity.getProducto().getImagen());
        
        // El precio que ve el cliente es el PRECIO BODEGUERO
        dto.setPrecioBodeguero(entity.getPrecioBodeguero()); 
        
        dto.setStock(entity.getStock());
        
        if (entity.getProducto().getCategoria() != null) {
            dto.setCategoriaNombre(entity.getProducto().getCategoria().getNombre());
            dto.setCategoriaId(entity.getProducto().getCategoria().getId());
        }
        return dto;
    }
}