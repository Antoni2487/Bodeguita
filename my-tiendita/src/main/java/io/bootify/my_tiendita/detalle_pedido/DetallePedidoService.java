package io.bootify.my_tiendita.detalle_pedido;

import io.bootify.my_tiendita.events.BeforeDeletePedido;
import io.bootify.my_tiendita.events.BeforeDeleteProducto;
import io.bootify.my_tiendita.pedido.Pedido;
import io.bootify.my_tiendita.pedido.PedidoRepository;
import io.bootify.my_tiendita.producto.Producto;
import io.bootify.my_tiendita.producto.ProductoRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.util.ReferencedException;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class DetallePedidoService {

    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public DetallePedidoService(final DetallePedidoRepository detallePedidoRepository,
            final PedidoRepository pedidoRepository, final ProductoRepository productoRepository) {
        this.detallePedidoRepository = detallePedidoRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    public List<DetallePedidoDTO> findAll() {
        final List<DetallePedido> detallePedidoes = detallePedidoRepository.findAll(Sort.by("id"));
        return detallePedidoes.stream()
                .map(detallePedido -> mapToDTO(detallePedido, new DetallePedidoDTO()))
                .toList();
    }

    public DetallePedidoDTO get(final Long id) {
        return detallePedidoRepository.findById(id)
                .map(detallePedido -> mapToDTO(detallePedido, new DetallePedidoDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final DetallePedidoDTO detallePedidoDTO) {
        final DetallePedido detallePedido = new DetallePedido();
        mapToEntity(detallePedidoDTO, detallePedido);
        return detallePedidoRepository.save(detallePedido).getId();
    }

    public void update(final Long id, final DetallePedidoDTO detallePedidoDTO) {
        final DetallePedido detallePedido = detallePedidoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(detallePedidoDTO, detallePedido);
        detallePedidoRepository.save(detallePedido);
    }

    public void delete(final Long id) {
        final DetallePedido detallePedido = detallePedidoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        detallePedidoRepository.delete(detallePedido);
    }

    private DetallePedidoDTO mapToDTO(final DetallePedido detallePedido,
            final DetallePedidoDTO detallePedidoDTO) {
        detallePedidoDTO.setId(detallePedido.getId());
        detallePedidoDTO.setCantidad(detallePedido.getCantidad());
        detallePedidoDTO.setSubtotal(detallePedido.getSubtotal());
        detallePedidoDTO.setPedido(detallePedido.getPedido() == null ? null : detallePedido.getPedido().getId());
        detallePedidoDTO.setProducto(detallePedido.getProducto() == null ? null : detallePedido.getProducto().getId());
        return detallePedidoDTO;
    }

    private DetallePedido mapToEntity(final DetallePedidoDTO detallePedidoDTO,
            final DetallePedido detallePedido) {
        detallePedido.setCantidad(detallePedidoDTO.getCantidad());
        detallePedido.setSubtotal(detallePedidoDTO.getSubtotal());
        final Pedido pedido = detallePedidoDTO.getPedido() == null ? null : pedidoRepository.findById(detallePedidoDTO.getPedido())
                .orElseThrow(() -> new NotFoundException("pedido not found"));
        detallePedido.setPedido(pedido);
        final Producto producto = detallePedidoDTO.getProducto() == null ? null : productoRepository.findById(detallePedidoDTO.getProducto())
                .orElseThrow(() -> new NotFoundException("producto not found"));
        detallePedido.setProducto(producto);
        return detallePedido;
    }

    @EventListener(BeforeDeletePedido.class)
    public void on(final BeforeDeletePedido event) {
        final ReferencedException referencedException = new ReferencedException();
        final DetallePedido pedidoDetallePedido = detallePedidoRepository.findFirstByPedidoId(event.getId());
        if (pedidoDetallePedido != null) {
            referencedException.setKey("pedido.detallePedido.pedido.referenced");
            referencedException.addParam(pedidoDetallePedido.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteProducto.class)
    public void on(final BeforeDeleteProducto event) {
        final ReferencedException referencedException = new ReferencedException();
        final DetallePedido productoDetallePedido = detallePedidoRepository.findFirstByProductoId(event.getId());
        if (productoDetallePedido != null) {
            referencedException.setKey("producto.detallePedido.producto.referenced");
            referencedException.addParam(productoDetallePedido.getId());
            throw referencedException;
        }
    }

}
