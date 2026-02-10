package io.bootify.my_tiendita.detalle_pedido;

import io.bootify.my_tiendita.events.BeforeDeletePedido; 
import io.bootify.my_tiendita.pedido.PedidoRepository;
import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaRepository;
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
    private final ProductoBodegaRepository productoBodegaRepository;

    public DetallePedidoService(final DetallePedidoRepository detallePedidoRepository,
            final PedidoRepository pedidoRepository, final ProductoBodegaRepository productoBodegaRepository) {
        this.detallePedidoRepository = detallePedidoRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoBodegaRepository = productoBodegaRepository;
    }

    public List<DetallePedidoDTO> findAll() {
        final List<DetallePedido> detallePedidos = detallePedidoRepository.findAll(Sort.by("id"));
        return detallePedidos.stream()
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

    // ==========================================
    // MAPPERS ACTUALIZADOS
    // ==========================================

    private DetallePedidoDTO mapToDTO(final DetallePedido detallePedido,
            final DetallePedidoDTO detallePedidoDTO) {
        
        // Mapeo básico
        // Nota: Como en el paso anterior definimos el DTO sin campo ID para el carrito,
        // si este DTO tiene ID, lo seteamos. Si no, comenta esta línea.
        // detallePedidoDTO.setId(detallePedido.getId()); 

        detallePedidoDTO.setCantidad(detallePedido.getCantidad());
        detallePedidoDTO.setSubtotal(detallePedido.getSubtotal());
        
        // Mapeo de relación ProductoBodega
        if (detallePedido.getProductoBodega() != null) {
            detallePedidoDTO.setProductoBodegaId(detallePedido.getProductoBodega().getId());
            
            // Info extra visual (si el DTO lo soporta)
            if (detallePedido.getProductoBodega().getProducto() != null) {
                detallePedidoDTO.setProductoNombre(detallePedido.getProductoBodega().getProducto().getNombre());
            }
            detallePedidoDTO.setPrecioUnitario(detallePedido.getPrecioUnitario());
        }
        
        return detallePedidoDTO;
    }

    private DetallePedido mapToEntity(final DetallePedidoDTO detallePedidoDTO,
            final DetallePedido detallePedido) {
        
        detallePedido.setCantidad(detallePedidoDTO.getCantidad());
        detallePedido.setSubtotal(detallePedidoDTO.getSubtotal());
        
        // Mapeo de ProductoBodega (Usamos el ID del DTO)
        if (detallePedidoDTO.getProductoBodegaId() != null && 
            (detallePedido.getProductoBodega() == null || !detallePedido.getProductoBodega().getId().equals(detallePedidoDTO.getProductoBodegaId()))) {
            
            final ProductoBodega productoBodega = productoBodegaRepository.findById(detallePedidoDTO.getProductoBodegaId())
                    .orElseThrow(() -> new NotFoundException("ProductoBodega no encontrado"));
            detallePedido.setProductoBodega(productoBodega);
            
            // Si es creación y no tiene precio, usamos el actual
            if (detallePedido.getPrecioUnitario() == null) {
                detallePedido.setPrecioUnitario(productoBodega.getPrecioBodeguero());
            }
        }
        
        // NOTA: No mapeamos 'Pedido' aquí porque lo retiramos del DTO para simplificar el carrito.
        // La creación de detalles se maneja principalmente desde PedidoService.
        
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
    
}