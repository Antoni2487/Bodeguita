package io.bootify.my_tiendita.inventario;

import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaRepository;
import io.bootify.my_tiendita.tipo_movimiento.TipoMovimiento;
import io.bootify.my_tiendita.tipo_movimiento.TipoMovimientoRepository;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class InventarioService {

    private final InventarioRepository movimientoRepo;
    private final ProductoBodegaRepository productoBodegaRepo;
    private final TipoMovimientoRepository tipoMovimientoRepo;
    private final UsuarioRepository usuarioRepo;

    private static final String NATURALEZA_ENTRADA = "ENTRADA";
    private static final String NATURALEZA_SALIDA = "SALIDA";
    private static final String MOVIMIENTO_VENTA = "VENTA"; 
    private static final String MOVIMIENTO_ANULACION = "ANULACION_VENTA";

    public InventarioService(
            InventarioRepository movimientoRepo,
            ProductoBodegaRepository productoBodegaRepo,
            TipoMovimientoRepository tipoMovimientoRepo,
            UsuarioRepository usuarioRepo) {
        this.movimientoRepo = movimientoRepo;
        this.productoBodegaRepo = productoBodegaRepo;
        this.tipoMovimientoRepo = tipoMovimientoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<InventarioDTO> obtenerHistorial(Long productoBodegaId) {
        ProductoBodega pb = productoBodegaRepo.findById(productoBodegaId)
                .orElseThrow(() -> new NotFoundException("Producto en bodega no encontrado"));

        return movimientoRepo.findByProductoBodegaOrderByFechaDesc(pb)
                .stream()
                .map(m -> mapToDTO(m, new InventarioDTO()))
                .toList();
    }

    /**
     * Método principal para registrar movimientos manuales o genéricos
     */
    public Long registrarMovimiento(InventarioDTO dto) {
        Inventario mov = new Inventario();
        mapToEntity(dto, mov); // Aquí ya cargamos productoBodega y tipoMovimiento
        
        mov.setFecha(LocalDateTime.now());

        // Lógica de Negocio Centralizada
        procesarMovimientoDeStock(mov);

        movimientoRepo.save(mov);
        return mov.getId();
    }

  
    public void registrarSalidaPorVenta(Long productoBodegaId, Integer cantidad, Long ventaId) {
        ProductoBodega pb = productoBodegaRepo.findById(productoBodegaId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado en bodega"));
        TipoMovimiento tipoVenta = tipoMovimientoRepo.findByNombre(MOVIMIENTO_VENTA)
                .orElseThrow(() -> new NotFoundException("El tipo de movimiento 'VENTA' no está configurado en la BD"));

       
        Inventario mov = new Inventario();
        mov.setProductoBodega(pb);
        mov.setTipoMovimiento(tipoVenta);
        mov.setCantidad(cantidad);
        mov.setReferenciaId(ventaId); 
        mov.setMotivo("Salida por Venta #" + ventaId);
        mov.setFecha(LocalDateTime.now());
        procesarMovimientoDeStock(mov);
        movimientoRepo.save(mov);
    }

 

    private void procesarMovimientoDeStock(Inventario mov) {
        ProductoBodega pb = mov.getProductoBodega();
        String naturaleza = mov.getTipoMovimiento().getNaturaleza(); // "ENTRADA" o "SALIDA"
        Integer cantidad = mov.getCantidad();

        if (NATURALEZA_SALIDA.equalsIgnoreCase(naturaleza)) {
            if (pb.getStock() < cantidad) {
                throw new IllegalArgumentException(
                    "Stock insuficiente para el producto: " + pb.getProducto().getNombre() + 
                    ". Stock actual: " + pb.getStock() + ", Solicitado: " + cantidad
                );
            }
            pb.setStock(pb.getStock() - cantidad);
        } else if (NATURALEZA_ENTRADA.equalsIgnoreCase(naturaleza)) {
            pb.setStock(pb.getStock() + cantidad);
        }

        productoBodegaRepo.save(pb);
    }

    public void reponerStock(Long productoBodegaId, Integer cantidad) {
        ProductoBodega pb = productoBodegaRepo.findById(productoBodegaId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado en bodega"));

        // Buscamos el tipo de movimiento. 
        // IMPORTANTE: Debes tener este tipo en tu Base de Datos con naturaleza 'ENTRADA'.
        TipoMovimiento tipoAnulacion = tipoMovimientoRepo.findByNombre(MOVIMIENTO_ANULACION)
                .orElseThrow(() -> new NotFoundException("El tipo de movimiento '" + MOVIMIENTO_ANULACION + "' no está configurado en la BD"));

        Inventario mov = new Inventario();
        mov.setProductoBodega(pb);
        mov.setTipoMovimiento(tipoAnulacion);
        mov.setCantidad(cantidad);
        mov.setMotivo("Devolución por Anulación de Venta");
        mov.setFecha(LocalDateTime.now());

        // Reutilizamos tu lógica central para sumar el stock
        procesarMovimientoDeStock(mov);
        
        movimientoRepo.save(mov);
    }

    
    private InventarioDTO mapToDTO(Inventario mov, InventarioDTO dto) {
        dto.setId(mov.getId());
        dto.setProductoBodegaId(mov.getProductoBodega().getId());
        dto.setTipoMovimientoId(mov.getTipoMovimiento().getId());
        dto.setCantidad(mov.getCantidad());
        dto.setMotivo(mov.getMotivo());
        dto.setReferenciaId(mov.getReferenciaId());
        dto.setUsuarioId(mov.getUsuario() != null ? mov.getUsuario().getId() : null);
        dto.setFecha(mov.getFecha());
        dto.setProductoNombre(mov.getProductoBodega().getProducto().getNombre());
        dto.setCategoriaNombre(mov.getProductoBodega().getProducto().getCategoria().getNombre());
        dto.setTipoMovimientoNombre(mov.getTipoMovimiento().getNombre());
        dto.setUsuarioNombre(mov.getUsuario() != null ? mov.getUsuario().getNombre() : "Sistema");
        return dto;
    }

    private void mapToEntity(InventarioDTO dto, Inventario mov) {
        mov.setProductoBodega(productoBodegaRepo.findById(dto.getProductoBodegaId())
                .orElseThrow(() -> new NotFoundException("Producto en bodega no encontrado")));
        
        mov.setTipoMovimiento(tipoMovimientoRepo.findById(dto.getTipoMovimientoId())
                .orElseThrow(() -> new NotFoundException("Tipo de movimiento no encontrado")));
                
        mov.setCantidad(dto.getCantidad());
        mov.setMotivo(dto.getMotivo());
        mov.setReferenciaId(dto.getReferenciaId());
        
        if (dto.getUsuarioId() != null) {
            mov.setUsuario(usuarioRepo.findById(dto.getUsuarioId()).orElse(null));
        }
    }
}