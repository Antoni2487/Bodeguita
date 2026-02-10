package io.bootify.my_tiendita.bodeguero;

import io.bootify.my_tiendita.bodega.*;
import io.bootify.my_tiendita.bodegaConfig.BodegaConfigDTO;
import io.bootify.my_tiendita.bodegaConfig.BodegaConfigService;
import io.bootify.my_tiendita.notificacion.Notificacion;
import io.bootify.my_tiendita.pago.BodegaMetodoPago;
import io.bootify.my_tiendita.notificacion.NotificacionService; 
import io.bootify.my_tiendita.pago.BodegaMetodoPagoRepository;
import io.bootify.my_tiendita.pago.BodegaMetodoPagoService;
import io.bootify.my_tiendita.pedido.PedidoDTO;
import io.bootify.my_tiendita.pedido.PedidoService; 
import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaRepository;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.bootify.my_tiendita.venta.Venta;
import io.bootify.my_tiendita.venta.VentaRepository;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Controller
public class BodegueroController {

    private final UsuarioRepository usuarioRepository;
    private final BodegaMetodoPagoService bodegaMetodoPagoService;
    private final BodegaRepository bodegaRepository;
    private final ProductoBodegaRepository productoBodegaRepository;
    private final BodegaConfigService bodegaConfigService;
    private final VentaRepository ventaRepository;
    private final BodegaMetodoPagoRepository bodegaMetodoPagoRepository;
    private final PedidoService pedidoService;
    private final NotificacionService notificacionService;

    public BodegueroController(UsuarioRepository usuarioRepository, 
                               BodegaMetodoPagoService bodegaMetodoPagoService,
                               BodegaRepository bodegaRepository,
                               ProductoBodegaRepository productoBodegaRepository,
                               BodegaConfigService bodegaConfigService,
                               VentaRepository ventaRepository,
                               BodegaMetodoPagoRepository bodegaMetodoPagoRepository,
                               PedidoService pedidoService,
                               NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
        this.productoBodegaRepository = productoBodegaRepository;
        this.bodegaConfigService = bodegaConfigService;
        this.ventaRepository = ventaRepository;
        this.bodegaMetodoPagoRepository = bodegaMetodoPagoRepository;
        this.pedidoService = pedidoService;
        this.notificacionService = notificacionService;
        this.bodegaMetodoPagoService = bodegaMetodoPagoService;
    }

    // --- DASHBOARD ---
    @GetMapping("/bodeguero/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository.findByEmailWithBodegas(auth.getName()).orElse(null);
        String nombre = usuario != null ? usuario.getNombre() : "Bodeguero";
        model.addAttribute("nombreBodeguero", nombre);
        return "bodeguero/dashboard";
    }

    // --- MIS PRODUCTOS ---
    @GetMapping("/bodeguero/mis_productos")
    @Transactional(readOnly = true)
    public String misProductos(Model model, @RequestParam(value = "bodegaId", required = false) Long bodegaId) {
        model.addAttribute("title", "Mis Productos");
        cargarDatosComunes(model, bodegaId);
        return "bodeguero/mis_productos";
    }

    // --- INVENTARIO ---
    @GetMapping("/bodeguero/inventario")
    @Transactional(readOnly = true)
    public String inventario(Model model, @RequestParam(value = "bodegaId", required = false) Long bodegaId) {
        model.addAttribute("title", "Gestión de Inventario");
        Bodega bodega = cargarDatosComunes(model, bodegaId);
        if (bodega != null) {
            model.addAttribute("productosBodega", productoBodegaRepository.findInventarioCompletoPorBodega(bodega.getId()));
        }
        return "bodeguero/inventario";
    }

    // =========================================================
    // ✅ GESTIÓN DE PEDIDOS (COLA FIFO) - LO QUE FALTABA
    // =========================================================
    @GetMapping("/bodeguero/pedidos")
    public String gestionPedidos(Model model, @RequestParam(value = "bodegaId", required = false) Long bodegaId) {
        model.addAttribute("title", "Cola de Pedidos");
        Bodega bodega = cargarDatosComunes(model, bodegaId);

        if (bodega != null) {
            // 1. Obtener el SIGUIENTE de la Cola (FIFO - Memoria)
            PedidoDTO siguiente = pedidoService.obtenerSiguientePedidoAAtender(bodega.getId());
            model.addAttribute("siguientePedido", siguiente);

            // 2. Obtener la lista de espera (FIFO - Memoria)
            List<PedidoDTO> colaPendientes = pedidoService.obtenerColaPendientes(bodega.getId());
            // Si el siguiente ya se muestra en grande, lo quitamos de la lista pequeña visual
            if (siguiente != null && !colaPendientes.isEmpty() && colaPendientes.get(0).getId().equals(siguiente.getId())) {
                colaPendientes.remove(0);
            }
            model.addAttribute("colaEspera", colaPendientes);

            // 3. Obtener Historial (Base de Datos)
            model.addAttribute("historial", pedidoService.obtenerHistorial(bodega.getId()));
        }
        
        return "bodeguero/pedidos";
    }

    // ✅ ACCIÓN: ATENDER PEDIDO (DESENCOLAR)
    @PostMapping("/bodeguero/pedidos/atender")
    public String atenderSiguiente(@RequestParam Long bodegaId, RedirectAttributes flash) {
        try {
            if (!validarAccesoBodega(bodegaId)) return "redirect:/bodeguero/dashboard";
            
            // Llama a la lógica que saca de la cola y crea la venta
            pedidoService.confirmarSiguientePedido(bodegaId);
            flash.addFlashAttribute("success", "¡Pedido atendido! Se generó la venta y descontó stock.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al atender: " + e.getMessage());
        }
        return "redirect:/bodeguero/pedidos?bodegaId=" + bodegaId;
    }

    // ✅ API JSON: NOTIFICACIONES (PILA LIFO)
    @GetMapping("/api/notificaciones/mis-alertas")
    @ResponseBody
    public ResponseEntity<List<Notificacion>> misNotificaciones() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository.findByEmailWithBodegas(auth.getName()).orElseThrow();
        // Devuelve la Pila convertida a Lista
        return ResponseEntity.ok(notificacionService.obtenerMisNotificaciones(usuario.getId()));
    }

    // --- CONFIGURACIÓN ---
    @GetMapping("/bodeguero/configuracion")
    public String verConfiguracion(Model model, @RequestParam(value = "bodegaId", required = false) Long bodegaId) {
        model.addAttribute("title", "Configuración de Bodega");
        Bodega bodega = cargarDatosComunes(model, bodegaId);
        if (bodega != null) {
            BodegaConfigDTO configDTO = bodegaConfigService.obtenerConfiguracion(bodega.getId());
            model.addAttribute("configuracion", configDTO);
            if (configDTO.getLatitud() == null) {
                configDTO.setLatitud(-6.77137);
                configDTO.setLongitud(-79.84088);
            }

        List<BodegaMetodoPago> metodosPago = bodegaMetodoPagoService.listarPorBodega(bodega.getId());
                model.addAttribute("metodosPago", metodosPago);
            }
            return "bodeguero/configuracion";
        }

    @PostMapping("/bodeguero/configuracion")
    public String guardarConfiguracion(@Valid @ModelAttribute("configuracion") BodegaConfigDTO configDTO,
                                       BindingResult bindingResult,
                                       @RequestParam("bodegaId") Long bodegaId,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (!validarAccesoBodega(bodegaId)) return "redirect:/bodeguero/dashboard";
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Configuración de Bodega");
            cargarDatosComunes(model, bodegaId);
            return "bodeguero/configuracion";
        }
        bodegaConfigService.guardarConfiguracion(bodegaId, configDTO);
        redirectAttributes.addFlashAttribute("mensaje", "¡Configuración guardada correctamente!");
        return "redirect:/bodeguero/configuracion?bodegaId=" + bodegaId;
    }

    @PostMapping("/api/bodeguero/pagos/actualizar")
    @ResponseBody
    public ResponseEntity<?> actualizarMetodoPago(
            @RequestParam("id") Long id,
            @RequestParam("numero") String numero,
            @RequestParam("titular") String titular,
            @RequestParam(value = "activo", required = false) Boolean activo,
            @RequestParam(value = "qr", required = false) MultipartFile qrFile) {
        
        try {
            // Delegamos al servicio la lógica de guardado y upload
            bodegaMetodoPagoService.actualizarMetodoPago(id, numero, titular, activo, qrFile);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Método de pago actualizado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }
 
    @GetMapping("/bodeguero/ventas")
    @Transactional(readOnly = true)
    public String ventas(Model model, @RequestParam(value = "bodegaId", required = false) Long bodegaId) {
        model.addAttribute("title", "Gestión de Ventas");
        
        Bodega bodegaSeleccionada = cargarDatosComunes(model, bodegaId);

        if (bodegaSeleccionada != null) {
            List<Venta> ventas = ventaRepository.findByBodegaId(bodegaSeleccionada.getId());
            model.addAttribute("ventas", ventas);
            
            double totalHoy = ventas.stream()
                .filter(v -> v.getFecha().toLocalDate().equals(java.time.LocalDate.now()))
                .mapToDouble(v -> v.getMonto().doubleValue())
                .sum();
            model.addAttribute("totalHoy", totalHoy);

            List<ProductoBodega> productosParaVender = productoBodegaRepository
                    .findProductosParaVenta(bodegaSeleccionada.getId());
            model.addAttribute("productos", productosParaVender);

            // 2. Métodos de Pago disponibles
            List<BodegaMetodoPago> metodosPago = bodegaMetodoPagoRepository
                    .findByBodegaId(bodegaSeleccionada.getId());
            model.addAttribute("metodosPago", metodosPago);
        }
        
        return "bodeguero/ventas";
    }

    // --- HELPER METHODS ---
    private Bodega cargarDatosComunes(Model model, Long bodegaIdRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository.findByEmailWithBodegas(auth.getName()).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        model.addAttribute("nombreBodeguero", usuario.getNombre());
        Set<Bodega> bodegas = usuario.getBodegas();

        if (bodegas.isEmpty()) {
            model.addAttribute("error", "No tienes bodegas asignadas.");
            return null;
        }
        model.addAttribute("bodegas", bodegas);
        Bodega bodegaSeleccionada = null;

        if (bodegas.size() == 1) {
            bodegaSeleccionada = bodegas.iterator().next();
        } else if (bodegaIdRequest != null) {
            bodegaSeleccionada = bodegas.stream().filter(b -> b.getId().equals(bodegaIdRequest)).findFirst().orElseThrow(() -> new RuntimeException("Bodega no autorizada"));
        } else {
             if(!bodegas.isEmpty()) bodegaSeleccionada = bodegas.iterator().next();
        }

        if (bodegaSeleccionada != null) {
            model.addAttribute("bodegaSeleccionada", bodegaSeleccionada);
            model.addAttribute("bodegaId", bodegaSeleccionada.getId());
            model.addAttribute("bodegaNombre", bodegaSeleccionada.getNombre());
        } else {
            model.addAttribute("mostrarSelector", true);
        }
        return bodegaSeleccionada;
    }

    private boolean validarAccesoBodega(Long bodegaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository.findByEmailWithBodegas(auth.getName()).orElse(null);
        if (usuario == null) return false;
        return usuario.getBodegas().stream().anyMatch(b -> b.getId().equals(bodegaId));
    }
}