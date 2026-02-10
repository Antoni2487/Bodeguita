package io.bootify.my_tiendita.producto_bodega;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.producto.Producto;
import io.bootify.my_tiendita.producto.ProductoRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.util.ReferencedException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@Transactional
public class ProductoBodegaService {

    private static final Logger log = LoggerFactory.getLogger(ProductoBodegaService.class);

    private final ProductoBodegaRepository productoBodegaRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;

    public ProductoBodegaService(final ProductoBodegaRepository productoBodegaRepository,
                                 final ProductoRepository productoRepository,
                                 final BodegaRepository bodegaRepository) {
        this.productoBodegaRepository = productoBodegaRepository;
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
    }

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los productos de una bodega específica
     */
    public List<ProductoBodegaDTO> findAllByBodega(final Long bodegaId) {
        log.debug("Obteniendo todos los productos de la bodega: {}", bodegaId);
        
        final List<ProductoBodega> productosBodega = productoBodegaRepository.findByBodegaId(bodegaId);
        
        return productosBodega.stream()
                .map(pb -> mapToDTO(pb, new ProductoBodegaDTO()))
                .toList();
    }

    /**
     * Obtener un producto específico de una bodega
     */
    public ProductoBodegaDTO get(final Long id) {
        log.debug("Obteniendo ProductoBodega con ID: {}", id);
        
        return productoBodegaRepository.findById(id)
                .map(pb -> mapToDTO(pb, new ProductoBodegaDTO()))
                .orElseThrow(() -> new NotFoundException("Producto no encontrado en esta bodega"));
    }

    /**
     * Agregar un producto del catálogo a una bodega
     * El bodeguero define el precio y stock inicial
     */
    public Long create(final ProductoBodegaDTO productoBodegaDTO) {
        log.info("Agregando producto {} a bodega {}", 
                productoBodegaDTO.getProducto(), 
                productoBodegaDTO.getBodega());
        
        // Validar que el producto no esté ya en la bodega
        if (productoBodegaRepository.existsByProductoIdAndBodegaId(
                productoBodegaDTO.getProducto(), 
                productoBodegaDTO.getBodega())) {
            throw new IllegalArgumentException("Este producto ya está asignado a la bodega");
        }
        
        final ProductoBodega productoBodega = new ProductoBodega();
        mapToEntity(productoBodegaDTO, productoBodega);
        
        return productoBodegaRepository.save(productoBodega).getId();
    }

    /**
     * Actualizar precio y/o stock de un producto en una bodega
     */
    public void update(final Long id, final ProductoBodegaDTO productoBodegaDTO) {
        log.info("Actualizando ProductoBodega con ID: {}", id);
        
        final ProductoBodega productoBodega = productoBodegaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado en esta bodega"));
        
        // Solo permitir actualizar precio, stock y estado activo
        // NO permitir cambiar el producto o la bodega
        productoBodega.setPrecioBodeguero(productoBodegaDTO.getPrecioBodeguero());
        productoBodega.setStock(productoBodegaDTO.getStock());
        productoBodega.setActivo(productoBodegaDTO.getActivo());
        
        productoBodegaRepository.save(productoBodega);
    }

    /**
     * Eliminar un producto de una bodega
     * Esto NO elimina el producto del catálogo, solo la asignación
     */
    public void delete(final Long id) {
        log.info("Eliminando ProductoBodega con ID: {}", id);
        
        final ProductoBodega productoBodega = productoBodegaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado en esta bodega"));
        
        // Aquí podrías agregar validaciones adicionales
        // Por ejemplo, no eliminar si hay pedidos pendientes
        
        productoBodegaRepository.delete(productoBodega);
    }

    // ==================== CONSULTAS ESPECIALES ====================

    /**
     * Obtener un producto específico de una bodega (por IDs)
     * Útil para verificar si un producto ya existe en una bodega
     */
    public ProductoBodegaDTO getByProductoAndBodega(final Long productoId, final Long bodegaId) {
        log.debug("Buscando producto {} en bodega {}", productoId, bodegaId);
        
        return productoBodegaRepository.findByProductoIdAndBodegaId(productoId, bodegaId)
                .map(pb -> mapToDTO(pb, new ProductoBodegaDTO()))
                .orElseThrow(() -> new NotFoundException("Producto no encontrado en esta bodega"));
    }

    /**
     * Buscar productos por nombre en una bodega específica
     */
    public List<ProductoBodegaDTO> searchByNombre(final Long bodegaId, final String nombre) {
        log.debug("Buscando productos con nombre '{}' en bodega {}", nombre, bodegaId);
        
        final List<ProductoBodega> productosBodega = 
                productoBodegaRepository.searchByBodegaIdAndProductoNombre(bodegaId, nombre);
        
        return productosBodega.stream()
                .map(pb -> mapToDTO(pb, new ProductoBodegaDTO()))
                .toList();
    }

    /**
     * Filtrar productos por categoría en una bodega
     */
    public List<ProductoBodegaDTO> findByCategoria(final Long bodegaId, final Long categoriaId) {
        log.debug("Filtrando productos de categoría {} en bodega {}", categoriaId, bodegaId);
        
        final List<ProductoBodega> productosBodega = 
                productoBodegaRepository.findByBodegaIdAndCategoriaId(bodegaId, categoriaId);
        
        return productosBodega.stream()
                .map(pb -> mapToDTO(pb, new ProductoBodegaDTO()))
                .toList();
    }

    /**
     * Obtener todos los productos activos de una bodega
     * Útil para mostrar solo productos disponibles para venta
     */
    public List<ProductoBodegaDTO> findActivosByBodega(final Long bodegaId) {
        log.debug("Obteniendo productos activos de la bodega: {}", bodegaId);
        
        final List<ProductoBodega> productosBodega = 
                productoBodegaRepository.findByBodegaIdAndActivoTrue(bodegaId);
        
        return productosBodega.stream()
                .map(pb -> mapToDTO(pb, new ProductoBodegaDTO()))
                .toList();
    }

    // ==================== MAPPERS ====================

    /**
     * Mapea de Entity a DTO incluyendo información adicional del producto
     */
    private ProductoBodegaDTO mapToDTO(final ProductoBodega productoBodega, 
                                       final ProductoBodegaDTO productoBodegaDTO) {
        productoBodegaDTO.setId(productoBodega.getId());
        productoBodegaDTO.setProducto(productoBodega.getProducto().getId());
        productoBodegaDTO.setBodega(productoBodega.getBodega().getId());
        productoBodegaDTO.setPrecioBodeguero(productoBodega.getPrecioBodeguero());
        productoBodegaDTO.setStock(productoBodega.getStock());
        productoBodegaDTO.setActivo(productoBodega.getActivo());
        
        
        final Producto producto = productoBodega.getProducto();
        productoBodegaDTO.setProductoNombre(producto.getNombre());
        productoBodegaDTO.setPrecioSugerido(producto.getPrecioSugerido());
        productoBodegaDTO.setProductoImagen(producto.getImagen());
        
        // Información de la categoría
        if (producto.getCategoria() != null) {
            productoBodegaDTO.setCategoriaId(producto.getCategoria().getId());
            productoBodegaDTO.setCategoriaNombre(producto.getCategoria().getNombre());
        }
        
        // Información de la bodega
        productoBodegaDTO.setBodegaNombre(productoBodega.getBodega().getNombre());
        
        return productoBodegaDTO;
    }

    /**
     * Mapea de DTO a Entity
     */
    private ProductoBodega mapToEntity(final ProductoBodegaDTO productoBodegaDTO, 
                                       final ProductoBodega productoBodega) {
        // Obtener producto del catálogo
        final Producto producto = productoBodegaDTO.getProducto() == null ? null :
                productoRepository.findById(productoBodegaDTO.getProducto())
                        .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        
        // Validar que el producto esté activo en el catálogo
        if (producto != null && !producto.getActivo()) {
            throw new IllegalArgumentException("No se puede agregar un producto inactivo");
        }
        
        productoBodega.setProducto(producto);
        
        // Obtener bodega
        final Bodega bodega = productoBodegaDTO.getBodega() == null ? null :
                bodegaRepository.findById(productoBodegaDTO.getBodega())
                        .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        
        productoBodega.setBodega(bodega);
        
        // Asignar precio, stock y estado
        productoBodega.setPrecioBodeguero(productoBodegaDTO.getPrecioBodeguero());
        productoBodega.setStock(productoBodegaDTO.getStock());
        productoBodega.setActivo(productoBodegaDTO.getActivo());
        
        return productoBodega;
    }

    // ==================== VALIDACIONES DE REFERENCIAS ====================

    /**
     * Verifica si un producto tiene asignaciones en bodegas
     * Se usa antes de eliminar un producto del catálogo
     */
    public boolean productoTieneAsignaciones(final Producto producto) {
        return productoBodegaRepository.findFirstByProducto(producto) != null;
    }

    /**
     * Verifica si una bodega tiene productos asignados
     * Se usa antes de eliminar una bodega
     */
    public boolean bodegaTieneProductos(final Bodega bodega) {
        return productoBodegaRepository.findFirstByBodega(bodega) != null;
    }

    /**
     * Lanza excepción si el producto tiene asignaciones
     */
    public void validarEliminacionProducto(final Producto producto) {
        final ProductoBodega productoBodega = productoBodegaRepository.findFirstByProducto(producto);
        if (productoBodega != null) {
            final ReferencedException exception = new ReferencedException();
            exception.setKey("producto.productoBodega.producto.referenced");
            exception.addParam(productoBodega.getId());
            throw exception;
        }
    }

    /**
     * Lanza excepción si la bodega tiene productos
     */
    public void validarEliminacionBodega(final Bodega bodega) {
        final ProductoBodega productoBodega = productoBodegaRepository.findFirstByBodega(bodega);
        if (productoBodega != null) {
            final ReferencedException exception = new ReferencedException();
            exception.setKey("bodega.productoBodega.bodega.referenced");
            exception.addParam(productoBodega.getId());
            throw exception;
        }
    }

}