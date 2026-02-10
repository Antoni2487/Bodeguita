package io.bootify.my_tiendita.pedido;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.bodegaConfig.BodegaConfig;
import io.bootify.my_tiendita.bodegaConfig.BodegaConfigRepository;
import io.bootify.my_tiendita.detalle_pedido.DetallePedido;
import io.bootify.my_tiendita.detalle_pedido.DetallePedidoDTO;
import io.bootify.my_tiendita.detalle_venta.DetalleVentaDTO;
import io.bootify.my_tiendita.estructuras.GestorColasPedidos;
import io.bootify.my_tiendita.events.BeforeDeletePedido;
import io.bootify.my_tiendita.model.EstadoPedido;
import io.bootify.my_tiendita.model.MetodoEntrega;
import io.bootify.my_tiendita.notificacion.NotificacionService;
import io.bootify.my_tiendita.pago.BodegaMetodoPagoDTO;
import io.bootify.my_tiendita.pago.BodegaMetodoPagoRepository;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaRepository;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.GeoUtils;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.venta.VentaDTO;
import io.bootify.my_tiendita.venta.VentaService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;
    private final VentaService ventaService;
    private final ProductoBodegaRepository productoBodegaRepository;
    private final NotificacionService notificacionService;
    private final BodegaMetodoPagoRepository bodegaMetodoPagoRepository;
    private final BodegaConfigRepository bodegaConfigRepository;
    private final GestorColasPedidos gestorColas;
    private final ApplicationEventPublisher publisher;

    public PedidoService(PedidoRepository pedidoRepository,
                         UsuarioRepository usuarioRepository,
                         BodegaRepository bodegaRepository,
                         VentaService ventaService,
                         NotificacionService notificacionService,
                         ProductoBodegaRepository productoBodegaRepository,
                         BodegaMetodoPagoRepository bodegaMetodoPagoRepository,
                         BodegaConfigRepository bodegaConfigRepository,
                         GestorColasPedidos gestorColas,
                         ApplicationEventPublisher publisher) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
        this.ventaService = ventaService;
        this.notificacionService = notificacionService;
        this.productoBodegaRepository = productoBodegaRepository;
        this.bodegaMetodoPagoRepository = bodegaMetodoPagoRepository;
        this.bodegaConfigRepository = bodegaConfigRepository;
        this.gestorColas = gestorColas;
        this.publisher = publisher;
    }

    @PostConstruct
    public void inicializarColas() {
        List<Pedido> pendientes = pedidoRepository.findByEstadoOrderByDateCreatedAsc(EstadoPedido.PENDIENTE);
        for (Pedido p : pendientes) {
            if (p.getBodega() != null) {
                gestorColas.encolar(p.getBodega().getId(), p);
            }
        }
    }

    // ==========================================
    // 🧠 LÓGICA DE PRE-CHECKOUT
    // ==========================================

    @Transactional(readOnly = true)
    public CheckoutResponseDTO validarPreCheckout(CheckoutRequestDTO req) {
        CheckoutResponseDTO response = new CheckoutResponseDTO();

        response.setSubtotal(BigDecimal.ZERO);
        response.setCostoDelivery(BigDecimal.ZERO);
        response.setTotal(BigDecimal.ZERO);
        response.setDistanciaKm(BigDecimal.ZERO);

        BodegaConfig config = bodegaConfigRepository.findByBodegaId(req.getBodegaId())
                .orElseThrow(() -> new NotFoundException("La bodega no tiene configuración de ventas activada."));

        if (!Boolean.TRUE.equals(config.getRealizaDelivery())) {
            response.setPosible(false);
            response.setMensaje("Esta bodega no realiza delivery por el momento.");
            return response;
        }

        Bodega bodega = config.getBodega();
        if (bodega.getLatitud() == null || bodega.getLongitud() == null) {
            throw new IllegalArgumentException("La bodega no tiene ubicación configurada en el sistema.");
        }

        double distancia = GeoUtils.calcularDistanciaKm(
                bodega.getLatitud(), bodega.getLongitud(),
                req.getLatitud(), req.getLongitud()
        );
        response.setDistanciaKm(BigDecimal.valueOf(distancia));

        if (config.getRadioMaximoKm() != null && 
            BigDecimal.valueOf(distancia).compareTo(config.getRadioMaximoKm()) > 0) {
            response.setPosible(false);
            response.setMensaje("Estás fuera del rango de reparto (" + config.getRadioMaximoKm() + "km máx).");
            return response;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (CheckoutRequestDTO.ItemCarritoDTO item : req.getProductos()) {
            var pb = productoBodegaRepository.findById(item.getProductoBodegaId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado."));
            
            if (pb.getStock() < item.getCantidad()) {
                response.setPosible(false);
                response.setMensaje("Stock insuficiente para: " + pb.getProducto().getNombre());
                return response;
            }

            BigDecimal precio = pb.getPrecioBodeguero(); 
            subtotal = subtotal.add(precio.multiply(new BigDecimal(item.getCantidad())));
        }
        
        if (config.getPedidoMinimoDelivery() != null && 
            subtotal.compareTo(config.getPedidoMinimoDelivery()) < 0) {
            response.setPosible(false);
            response.setMensaje("El pedido mínimo es de S/ " + config.getPedidoMinimoDelivery());
            response.setSubtotal(subtotal);
            return response;
        }

        BigDecimal costoEnvio = BigDecimal.ZERO;
        if (config.getPrecioPorKm() != null) {
            costoEnvio = config.getPrecioPorKm().multiply(BigDecimal.valueOf(distancia));
            costoEnvio = costoEnvio.setScale(2, RoundingMode.HALF_UP);
        }

        response.setPosible(true);
        response.setMensaje("Cobertura válida");
        response.setSubtotal(subtotal);
        response.setCostoDelivery(costoEnvio);
        response.setTotal(subtotal.add(costoEnvio));
        
        List<BodegaMetodoPagoDTO> metodosDTO = bodegaMetodoPagoRepository.findByBodegaId(req.getBodegaId())
                .stream()
                .filter(mp -> Boolean.TRUE.equals(mp.getActivo()))
                .map(mp -> {
                    BodegaMetodoPagoDTO dto = new BodegaMetodoPagoDTO();
                    dto.setId(mp.getId());
                    dto.setNombreTitular(mp.getNombreTitular());
                    dto.setNumeroTelefono(mp.getNumeroTelefono());
                    dto.setImagenQrUrl(mp.getImagenQrUrl());
                    if (mp.getTipoMetodoPago() != null) {
                        dto.setTipoMetodoPagoId(mp.getTipoMetodoPago().getId());
                        dto.setTipoMetodoPagoNombre(mp.getTipoMetodoPago().getNombre());
                    }
                    return dto;
                }).collect(Collectors.toList());
        
        response.setMetodosPago(metodosDTO);

        return response;
    }

    // ==========================================
    // CRUD
    // ==========================================

    public List<PedidoDTO> findAll() {
        return pedidoRepository.findAll(Sort.by("id"))
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public PedidoDTO get(final Long id) {
        return pedidoRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void update(final Long id, final PedidoDTO pedidoDTO) {
        final Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        
        if(pedidoDTO.getEstado() != null) pedido.setEstado(EstadoPedido.valueOf(pedidoDTO.getEstado()));
        if(pedidoDTO.getDireccionEntrega() != null) pedido.setDireccionEntrega(pedidoDTO.getDireccionEntrega());
        
        pedidoRepository.save(pedido);
    }

    public void delete(final Long id) {
        final Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeletePedido(id));
        pedidoRepository.delete(pedido);
    }

    public Map<Long, String> getPedidoValues() {
        return pedidoRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Pedido::getId, Pedido::getCodigoPedido));
    }

    // ==========================================
    // CREACIÓN DE PEDIDO
    // ==========================================

    @Transactional
    public Long create(PedidoDTO pedidoDTO) {
        final Pedido pedido = new Pedido();
        
        Bodega bodega = bodegaRepository.findById(pedidoDTO.getBodega())
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        
        Usuario usuario = usuarioRepository.findById(pedidoDTO.getUsuario())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        pedido.setBodega(bodega);
        pedido.setUsuario(usuario);
        pedido.setDireccionEntrega(pedidoDTO.getDireccionEntrega());
        pedido.setTelefonoContacto(pedidoDTO.getTelefonoContacto());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setCodigoPedido("PED-" + System.currentTimeMillis()); 
        pedido.setFechaPedido(java.time.LocalTime.now());

    
        BigDecimal totalCalculado = BigDecimal.ZERO;
        
        if (pedidoDTO.getDetalles() != null && !pedidoDTO.getDetalles().isEmpty()) {
            for (DetallePedidoDTO itemDTO : pedidoDTO.getDetalles()) {
                var pb = productoBodegaRepository.findById(itemDTO.getProductoBodegaId())
                        .orElseThrow(() -> new NotFoundException("Producto no disponible."));
                
                if (!pb.getBodega().getId().equals(bodega.getId())) {
                    throw new IllegalArgumentException("Producto de otra bodega.");
                }

                if (pb.getStock() < itemDTO.getCantidad()) {
                      throw new IllegalArgumentException("Stock insuficiente.");
                }

                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedido);
                detalle.setProductoBodega(pb);
                detalle.setCantidad(itemDTO.getCantidad());
                detalle.setDateCreated(java.time.OffsetDateTime.now());
                detalle.setLastUpdated(java.time.OffsetDateTime.now());
                
                BigDecimal precioActual = pb.getPrecioBodeguero();
                detalle.setPrecioUnitario(precioActual);
                
                BigDecimal subtotal = precioActual.multiply(new BigDecimal(itemDTO.getCantidad()));
                detalle.setSubtotal(subtotal);
                
                pedido.addDetalle(detalle);
                totalCalculado = totalCalculado.add(subtotal);
            }
        }
        
        pedido.setTotal(totalCalculado);
        
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        gestorColas.encolar(bodega.getId(), pedidoGuardado);
        
        if(bodega.getUsuario() != null) {
            notificacionService.notificar(
                bodega.getUsuario().getId(),
                "Nuevo Pedido #" + pedidoGuardado.getCodigoPedido(),
                "PEDIDO",
                "/bodeguero/pedidos"
            );
        }

        return pedidoGuardado.getId();
    }

    // ==========================================
    // COLAS Y CONFIRMACIÓN
    // ==========================================

    @Transactional(readOnly = true)
    public PedidoDTO obtenerSiguientePedidoAAtender(Long bodegaId) {
        Pedido siguienteEnMemoria = gestorColas.verSiguiente(bodegaId);
        
        if (siguienteEnMemoria != null) {
           
            return pedidoRepository.findById(siguienteEnMemoria.getId())
                    .map(this::mapToDTO)
                    .orElse(null);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> obtenerColaPendientes(Long bodegaId) {
        return gestorColas.obtenerCola(bodegaId).stream()
            
                .map(p -> pedidoRepository.findById(p.getId()).orElse(null))
                .filter(Objects::nonNull)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true) 
    public List<PedidoDTO> obtenerHistorial(Long bodegaId) {
    
        return pedidoRepository.findHistorialConRelaciones(bodegaId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmarSiguientePedido(Long bodegaId) {
        Pedido pedido = gestorColas.desencolar(bodegaId);
        if (pedido == null) throw new IllegalArgumentException("La cola está vacía.");
        confirmarPedidoLogica(pedido);
    }

    private void confirmarPedidoLogica(Pedido pedido) {
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        
        VentaDTO ventaDTO = new VentaDTO();
        ventaDTO.setBodegaId(pedido.getBodega().getId());
        ventaDTO.setClienteNombre(pedido.getUsuario().getNombre() + " (Web)");
        ventaDTO.setTipoEntrega(MetodoEntrega.DELIVERY);
        ventaDTO.setDireccionEntrega(pedido.getDireccionEntrega());
        ventaDTO.setMonto(pedido.getTotal());
        
        // ✅ CORRECCIÓN: Ahora VentaDTO tiene los métodos necesarios
        if (pedido.getLatitudEntrega() != null) ventaDTO.setLatitudEntrega(pedido.getLatitudEntrega());
        if (pedido.getLongitudEntrega() != null) ventaDTO.setLongitudEntrega(pedido.getLongitudEntrega());
        
        if (pedido.getMetodoPago() != null) {
            ventaDTO.setBodegaMetodoPagoId(pedido.getMetodoPago().getId());
        } else {
            var metodoPago = bodegaMetodoPagoRepository.findByBodegaId(pedido.getBodega().getId())
                    .stream().findFirst().orElseThrow(() -> new NotFoundException("Sin métodos de pago configurados"));
            ventaDTO.setBodegaMetodoPagoId(metodoPago.getId());
        }

        List<DetalleVentaDTO> productosVenta = new ArrayList<>();
        for(DetallePedido dp : pedido.getDetalles()) {
             DetalleVentaDTO dv = new DetalleVentaDTO();
             dv.setProductoBodegaId(dp.getProductoBodega().getId()); 
             dv.setCantidad(dp.getCantidad());
             dv.setPrecioUnitario(dp.getPrecioUnitario());
             dv.setSubtotal(dp.getSubtotal());
             productosVenta.add(dv);
        }
        
        ventaDTO.setDetalles(productosVenta);
        ventaService.create(ventaDTO); 
        
        pedidoRepository.save(pedido);
    }

    // ==========================================
    // MANUAL Y MAPPER
    // ==========================================
    
    @Transactional
    public void crearPedidoManual(PedidoManualDTO dto) {
        Bodega bodega = bodegaRepository.findById(dto.getBodegaId())
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado"));

        Pedido pedido = new Pedido();
        pedido.setBodega(bodega);
        pedido.setUsuario(usuario);
        pedido.setDireccionEntrega(dto.getDireccionEntrega());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setCodigoPedido("MAN-" + System.currentTimeMillis());
        pedido.setFechaPedido(java.time.LocalTime.now());

        BigDecimal total = BigDecimal.ZERO;
        
        for (PedidoManualDTO.ItemManualDTO item : dto.getProductos()) {
            var pb = productoBodegaRepository.findById(item.getProductoBodegaId())
                    .orElseThrow(() -> new NotFoundException("Producto Bodega no encontrado"));
            
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProductoBodega(pb);
            detalle.setCantidad(item.getCantidad());
            
            BigDecimal precio = pb.getPrecioBodeguero(); 
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(precio.multiply(new BigDecimal(item.getCantidad())));
            
            pedido.addDetalle(detalle);
            total = total.add(detalle.getSubtotal());
        }
        
        pedido.setTotal(total);
        Pedido guardado = pedidoRepository.save(pedido);
        gestorColas.encolar(bodega.getId(), guardado);
    }
    public List<PedidoDTO> findAllByUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByDateCreatedDesc(usuarioId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public boolean ventaExists(final Long id) {
        return pedidoRepository.existsByVentaId(id);
    }

    private PedidoDTO mapToDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setCodigoPedido(pedido.getCodigoPedido());
        // Manejo seguro de fecha (si viene nula por algún motivo antiguo)
        if(pedido.getDateCreated() != null) {
            dto.setFechaPedido(pedido.getDateCreated());
        }
        
        dto.setTotal(pedido.getTotal());
        dto.setDireccionEntrega(pedido.getDireccionEntrega());
        dto.setTelefonoContacto(pedido.getTelefonoContacto());
        dto.setEstado(pedido.getEstado().name());
    
        if (pedido.getUsuario() != null) {
            dto.setUsuario(pedido.getUsuario().getId());
            dto.setUsuarioNombre(pedido.getUsuario().getNombre());
        } else {
            dto.setUsuarioNombre("Cliente Desconocido");
        }
        
        if (pedido.getBodega() != null) {
            dto.setBodega(pedido.getBodega().getId());
        }
        
        dto.setVenta(pedido.getVenta() != null ? pedido.getVenta().getId() : null);
        return dto;
    }
}