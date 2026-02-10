package io.bootify.my_tiendita.venta;

import io.bootify.my_tiendita.bodega.*;
import io.bootify.my_tiendita.bodegaConfig.BodegaConfig;
import io.bootify.my_tiendita.bodegaConfig.BodegaConfigRepository;
import io.bootify.my_tiendita.detalle_venta.*;
import io.bootify.my_tiendita.inventario.InventarioService;
import io.bootify.my_tiendita.model.MetodoEntrega;
import io.bootify.my_tiendita.pago.BodegaMetodoPago;
import io.bootify.my_tiendita.pago.BodegaMetodoPagoRepository;
import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaRepository;
import io.bootify.my_tiendita.util.GeoUtils;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.events.BeforeDeleteVenta;
import io.bootify.my_tiendita.util.CustomCollectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoBodegaRepository productoBodegaRepository;
    private final BodegaRepository bodegaRepository;
    private final BodegaConfigRepository bodegaConfigRepository;
    private final BodegaMetodoPagoRepository bodegaMetodoPagoRepository;
    private final InventarioService inventarioService;
    private final ApplicationEventPublisher publisher;

    public VentaService(
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            ProductoBodegaRepository productoBodegaRepository,
            BodegaRepository bodegaRepository,
            BodegaConfigRepository bodegaConfigRepository,
            BodegaMetodoPagoRepository bodegaMetodoPagoRepository,
            InventarioService inventarioService,
            ApplicationEventPublisher publisher) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoBodegaRepository = productoBodegaRepository;
        this.bodegaRepository = bodegaRepository;
        this.bodegaConfigRepository = bodegaConfigRepository;
        this.bodegaMetodoPagoRepository = bodegaMetodoPagoRepository;
        this.inventarioService = inventarioService;
        this.publisher = publisher;
    }

    // --- MÉTODOS DE LECTURA ---
    public List<VentaDTO> findAll() {
        final List<Venta> ventas = ventaRepository.findAll(Sort.by("id"));
        return ventas.stream()
                .map(venta -> mapToDTO(venta, new VentaDTO()))
                .toList();
    }

    @Transactional // Importante para traer los detalles Lazy
    public VentaDTO get(final Long id) {
        return ventaRepository.findById(id)
                .map(venta -> mapToDTO(venta, new VentaDTO()))
                .orElseThrow(NotFoundException::new);
    }

    // --- CREAR VENTA (LÓGICA CORE) ---
    @Transactional
    public Long create(final VentaDTO ventaDTO) {

        Bodega bodega = bodegaRepository.findById(ventaDTO.getBodegaId())
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));

        BodegaMetodoPago metodoPagoConfig = bodegaMetodoPagoRepository.findById(ventaDTO.getBodegaMetodoPagoId())
                .orElseThrow(() -> new NotFoundException("Método de pago no válido"));

        if (!metodoPagoConfig.getBodega().getId().equals(bodega.getId())) {
            throw new IllegalArgumentException("El método de pago no corresponde a esta bodega");
        }

        final Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setEstado("COMPLETADA"); // ✅ IMPORTANTE: Estado inicial
        venta.setTipoMetodoPago(metodoPagoConfig.getTipoMetodoPago());
        venta.setTipoEntrega(ventaDTO.getTipoEntrega());
        venta.setBodegaMetodoPago(metodoPagoConfig); // Vincular relación
        
        // Guardar nombre cliente si viene (Para el ticket)
        if(ventaDTO.getNombreCliente() != null) {
             venta.setClienteNombre(ventaDTO.getNombreCliente());
        } else if (ventaDTO.getClienteNombre() != null) {
             venta.setClienteNombre(ventaDTO.getClienteNombre());
        }

        BigDecimal costoDelivery = BigDecimal.ZERO;

        if (ventaDTO.getTipoEntrega() == MetodoEntrega.DELIVERY) {

            BodegaConfig config = bodegaConfigRepository.findByBodegaId(bodega.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Esta bodega no tiene configurado el delivery"));

            if (Boolean.FALSE.equals(config.getRealizaDelivery())) {
                throw new IllegalArgumentException("Esta bodega no realiza delivery actualmente");
            }

            if (ventaDTO.getLatitudEntrega() == null || ventaDTO.getLongitudEntrega() == null) {
                throw new IllegalArgumentException("Se requieren coordenadas para el delivery");
            }

            double distanciaKm = GeoUtils.calcularDistanciaKm(
                    bodega.getLatitud(), bodega.getLongitud(),
                    ventaDTO.getLatitudEntrega(), ventaDTO.getLongitudEntrega()
            );

            if (config.getRadioMaximoKm() != null && BigDecimal.valueOf(distanciaKm).compareTo(config.getRadioMaximoKm()) > 0) {
                throw new IllegalArgumentException("Tu ubicación está fuera del rango de reparto (" + config.getRadioMaximoKm() + " km)");
            }

            BigDecimal precioKm = config.getPrecioPorKm() != null ? config.getPrecioPorKm() : BigDecimal.ZERO;
            costoDelivery = precioKm.multiply(BigDecimal.valueOf(distanciaKm));

            venta.setCostoDelivery(costoDelivery);
            venta.setLatitudEntrega(ventaDTO.getLatitudEntrega());
            venta.setLongitudEntrega(ventaDTO.getLongitudEntrega());
            venta.setDireccionEntrega(ventaDTO.getDireccionEntrega());

        } else {
            venta.setCostoDelivery(BigDecimal.ZERO);
        }

        venta.setMonto(BigDecimal.ZERO);
        Venta ventaGuardada = ventaRepository.save(venta);

        BigDecimal totalProductos = BigDecimal.ZERO;
        List<DetalleVenta> detallesParaGuardar = new ArrayList<>();

        if (ventaDTO.getProductos() == null || ventaDTO.getProductos().isEmpty()) {
            throw new IllegalArgumentException("El carrito no puede estar vacío");
        }

        for (DetalleVentaDTO item : ventaDTO.getProductos()) {
            ProductoBodega pb = productoBodegaRepository.findById(item.getProductoBodegaId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.getProductoBodegaId()));

            if (!pb.getBodega().getId().equals(bodega.getId())) {
                throw new IllegalArgumentException("El producto " + pb.getProducto().getNombre() + " es de otra bodega");
            }

            // Descontar Stock
            inventarioService.registrarSalidaPorVenta(pb.getId(), item.getCantidad(), ventaGuardada.getId());

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(ventaGuardada);
            detalle.setProductoBodega(pb);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(pb.getPrecioBodeguero());

            BigDecimal subtotal = pb.getPrecioBodeguero().multiply(BigDecimal.valueOf(item.getCantidad()));
            detalle.setSubtotal(subtotal);

            totalProductos = totalProductos.add(subtotal);
            detallesParaGuardar.add(detalle);
        }

        if (ventaDTO.getTipoEntrega() == MetodoEntrega.DELIVERY) {
            BodegaConfig config = bodegaConfigRepository.findByBodegaId(bodega.getId()).orElse(null);
            if (config != null && config.getPedidoMinimoDelivery() != null) {
                if (totalProductos.compareTo(config.getPedidoMinimoDelivery()) < 0) {
                    throw new IllegalArgumentException("El pedido mínimo para delivery es S/ " + config.getPedidoMinimoDelivery());
                }
            }
        }

        detalleVentaRepository.saveAll(detallesParaGuardar);

        ventaGuardada.setMonto(totalProductos.add(costoDelivery));
        ventaRepository.save(ventaGuardada);

        return ventaGuardada.getId();
    }

    @Transactional
    public void anular(final Long id) {
        final Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venta no encontrada"));

        if ("ANULADA".equals(venta.getEstado())) {
            throw new IllegalArgumentException("Esta venta ya está anulada");
        }

        // 1. Devolver Stock al Inventario
        for (DetalleVenta detalle : venta.getDetalles()) {
            // Nota: Debes tener un método 'reponerStock' o 'registrarIngreso' en InventarioService
            // Si no lo tienes, usa 'registrarIngreso' pasando un motivo.
            inventarioService.reponerStock(detalle.getProductoBodega().getId(), detalle.getCantidad());
        }

        // 2. Cambiar estado
        venta.setEstado("ANULADA");
        ventaRepository.save(venta);
    }

    public void update(final Long id, final VentaDTO ventaDTO) {
        final Venta venta = ventaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        // Validar lógica de update si es necesaria
        ventaRepository.save(venta);
    }

    public void delete(final Long id) {
        final Venta venta = ventaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteVenta(id));
        ventaRepository.delete(venta);
    }

    // --- MAPPER ---
    private VentaDTO mapToDTO(final Venta venta, final VentaDTO ventaDTO) {
        ventaDTO.setId(venta.getId());
        ventaDTO.setMonto(venta.getMonto()); // Asegúrate que el DTO use 'monto' o 'montoTotal' consistentemente
        ventaDTO.setMontoTotal(venta.getMonto()); // Por si acaso
        if (venta.getFecha() != null) {
            ventaDTO.setFecha(venta.getFecha());
        } else if (venta.getDateCreated() != null) {
            ventaDTO.setFecha(venta.getDateCreated().toLocalDateTime());
        }
        ventaDTO.setTipoEntrega(venta.getTipoEntrega());
        ventaDTO.setCostoDelivery(venta.getCostoDelivery());
        ventaDTO.setClienteNombre(venta.getClienteNombre());
        ventaDTO.setEstado(venta.getEstado()); 

  
        if (venta.getTipoMetodoPago() != null) {
            
            ventaDTO.setTipoMetodoPago(venta.getTipoMetodoPago().getNombre());
            ventaDTO.setNombreMetodoPago(venta.getTipoMetodoPago().getNombre());
        }

        // Mapear detalles completos (Productos)
        if (venta.getDetalles() != null) {
            List<DetalleVentaDTO> detallesDTO = venta.getDetalles().stream().map(d -> {
                DetalleVentaDTO dto = new DetalleVentaDTO();
                dto.setProductoBodegaId(d.getProductoBodega().getId());
                dto.setCantidad(d.getCantidad());
                
                // Mapeo seguro de propiedades anidadas
                if (d.getProductoBodega().getProducto() != null) {
                    dto.setNombreProducto(d.getProductoBodega().getProducto().getNombre());
                    dto.setImagenProductoUrl(d.getProductoBodega().getProducto().getImagen()); // O getImagenUrl() según tu entidad
                } else {
                    dto.setNombreProducto("Producto eliminado");
                }
                
                dto.setPrecioUnitario(d.getPrecioUnitario());
                dto.setSubtotal(d.getSubtotal());
                return dto;
            }).toList();
            
            ventaDTO.setProductos(detallesDTO);
            ventaDTO.setDetalles(detallesDTO); // Por compatibilidad si tienes ambos campos

            if (!detallesDTO.isEmpty() && venta.getDetalles().get(0).getProductoBodega() != null) {
                ventaDTO.setBodegaId(venta.getDetalles().get(0).getProductoBodega().getBodega().getId());
            }
        }

        return ventaDTO;
    }

    public Map<Long, Long> getVentaValues() {
        return ventaRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Venta::getId, Venta::getId));
    }
}