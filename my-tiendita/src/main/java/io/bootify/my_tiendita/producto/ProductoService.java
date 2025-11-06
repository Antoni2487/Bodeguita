package io.bootify.my_tiendita.producto;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.categoria.Categoria;
import io.bootify.my_tiendita.categoria.CategoriaRepository;
import io.bootify.my_tiendita.events.BeforeDeleteBodega;
import io.bootify.my_tiendita.events.BeforeDeleteProducto;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.util.ReferencedException;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ApplicationEventPublisher publisher;

    public ProductoService(final ProductoRepository productoRepository,
            final BodegaRepository bodegaRepository, final CategoriaRepository categoriaRepository,
            final ApplicationEventPublisher publisher) {
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
        this.categoriaRepository = categoriaRepository;
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
        mapToEntity(productoDTO, producto);
        productoRepository.save(producto);
    }

    public void delete(final Long id) {
        final Producto producto = productoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteProducto(id));
        productoRepository.delete(producto);
    }

    private ProductoDTO mapToDTO(final Producto producto, final ProductoDTO productoDTO) {
        productoDTO.setId(producto.getId());
        productoDTO.setNombre(producto.getNombre());
        productoDTO.setDescripcion(producto.getDescripcion());
        productoDTO.setPrecio(producto.getPrecio());
        productoDTO.setStock(producto.getStock());
        productoDTO.setImagen(producto.getImagen());
        productoDTO.setActivo(producto.getActivo());
        productoDTO.setBodega(producto.getBodega() == null ? null : producto.getBodega().getId());
        productoDTO.setCategoria(producto.getCategoria() == null ? null : producto.getCategoria().getId());
        return productoDTO;
    }

    private Producto mapToEntity(final ProductoDTO productoDTO, final Producto producto) {
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecio(productoDTO.getPrecio());
        producto.setStock(productoDTO.getStock());
        producto.setImagen(productoDTO.getImagen());
        producto.setActivo(productoDTO.getActivo());
        final Bodega bodega = productoDTO.getBodega() == null ? null : bodegaRepository.findById(productoDTO.getBodega())
                .orElseThrow(() -> new NotFoundException("bodega not found"));
        producto.setBodega(bodega);
        final Categoria categoria = productoDTO.getCategoria() == null ? null : categoriaRepository.findById(productoDTO.getCategoria())
                .orElseThrow(() -> new NotFoundException("categoria not found"));
        producto.setCategoria(categoria);
        return producto;
    }

    public Map<Long, String> getProductoValues() {
        return productoRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Producto::getId, Producto::getNombre));
    }

    @EventListener(BeforeDeleteBodega.class)
    public void on(final BeforeDeleteBodega event) {
        final ReferencedException referencedException = new ReferencedException();
        final Producto bodegaProducto = productoRepository.findFirstByBodegaId(event.getId());
        if (bodegaProducto != null) {
            referencedException.setKey("bodega.producto.bodega.referenced");
            referencedException.addParam(bodegaProducto.getId());
            throw referencedException;
        }
    }

}
