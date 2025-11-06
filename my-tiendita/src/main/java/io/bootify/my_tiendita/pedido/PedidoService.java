package io.bootify.my_tiendita.pedido;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.events.BeforeDeleteBodega;
import io.bootify.my_tiendita.events.BeforeDeletePedido;
import io.bootify.my_tiendita.events.BeforeDeleteUsuario;
import io.bootify.my_tiendita.events.BeforeDeleteVenta;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.util.ReferencedException;
import io.bootify.my_tiendita.venta.Venta;
import io.bootify.my_tiendita.venta.VentaRepository;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;
    private final VentaRepository ventaRepository;
    private final ApplicationEventPublisher publisher;

    public PedidoService(final PedidoRepository pedidoRepository,
            final UsuarioRepository usuarioRepository, final BodegaRepository bodegaRepository,
            final VentaRepository ventaRepository, final ApplicationEventPublisher publisher) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
        this.ventaRepository = ventaRepository;
        this.publisher = publisher;
    }

    public List<PedidoDTO> findAll() {
        final List<Pedido> pedidoes = pedidoRepository.findAll(Sort.by("id"));
        return pedidoes.stream()
                .map(pedido -> mapToDTO(pedido, new PedidoDTO()))
                .toList();
    }

    public PedidoDTO get(final Long id) {
        return pedidoRepository.findById(id)
                .map(pedido -> mapToDTO(pedido, new PedidoDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final PedidoDTO pedidoDTO) {
        final Pedido pedido = new Pedido();
        mapToEntity(pedidoDTO, pedido);
        return pedidoRepository.save(pedido).getId();
    }

    public void update(final Long id, final PedidoDTO pedidoDTO) {
        final Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(pedidoDTO, pedido);
        pedidoRepository.save(pedido);
    }

    public void delete(final Long id) {
        final Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeletePedido(id));
        pedidoRepository.delete(pedido);
    }

    private PedidoDTO mapToDTO(final Pedido pedido, final PedidoDTO pedidoDTO) {
        pedidoDTO.setId(pedido.getId());
        pedidoDTO.setFechaPedido(pedido.getFechaPedido());
        pedidoDTO.setTotal(pedido.getTotal());
        pedidoDTO.setDireccionEntrega(pedido.getDireccionEntrega());
        pedidoDTO.setUsuario(pedido.getUsuario() == null ? null : pedido.getUsuario().getId());
        pedidoDTO.setBodega(pedido.getBodega() == null ? null : pedido.getBodega().getId());
        pedidoDTO.setVenta(pedido.getVenta() == null ? null : pedido.getVenta().getId());
        return pedidoDTO;
    }

    private Pedido mapToEntity(final PedidoDTO pedidoDTO, final Pedido pedido) {
        pedido.setFechaPedido(pedidoDTO.getFechaPedido());
        pedido.setTotal(pedidoDTO.getTotal());
        pedido.setDireccionEntrega(pedidoDTO.getDireccionEntrega());
        final Usuario usuario = pedidoDTO.getUsuario() == null ? null : usuarioRepository.findById(pedidoDTO.getUsuario())
                .orElseThrow(() -> new NotFoundException("usuario not found"));
        pedido.setUsuario(usuario);
        final Bodega bodega = pedidoDTO.getBodega() == null ? null : bodegaRepository.findById(pedidoDTO.getBodega())
                .orElseThrow(() -> new NotFoundException("bodega not found"));
        pedido.setBodega(bodega);
        final Venta venta = pedidoDTO.getVenta() == null ? null : ventaRepository.findById(pedidoDTO.getVenta())
                .orElseThrow(() -> new NotFoundException("venta not found"));
        pedido.setVenta(venta);
        return pedido;
    }

    public boolean ventaExists(final Long id) {
        return pedidoRepository.existsByVentaId(id);
    }

    public Map<Long, String> getPedidoValues() {
        return pedidoRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Pedido::getId, Pedido::getDireccionEntrega));
    }

    @EventListener(BeforeDeleteUsuario.class)
    public void on(final BeforeDeleteUsuario event) {
        final ReferencedException referencedException = new ReferencedException();
        final Pedido usuarioPedido = pedidoRepository.findFirstByUsuarioId(event.getId());
        if (usuarioPedido != null) {
            referencedException.setKey("usuario.pedido.usuario.referenced");
            referencedException.addParam(usuarioPedido.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteBodega.class)
    public void on(final BeforeDeleteBodega event) {
        final ReferencedException referencedException = new ReferencedException();
        final Pedido bodegaPedido = pedidoRepository.findFirstByBodegaId(event.getId());
        if (bodegaPedido != null) {
            referencedException.setKey("bodega.pedido.bodega.referenced");
            referencedException.addParam(bodegaPedido.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeleteVenta.class)
    public void on(final BeforeDeleteVenta event) {
        final ReferencedException referencedException = new ReferencedException();
        final Pedido ventaPedido = pedidoRepository.findFirstByVentaId(event.getId());
        if (ventaPedido != null) {
            referencedException.setKey("venta.pedido.venta.referenced");
            referencedException.addParam(ventaPedido.getId());
            throw referencedException;
        }
    }

}
