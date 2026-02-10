package io.bootify.my_tiendita.cliente;

import io.bootify.my_tiendita.bodega.BodegaDTO;
import io.bootify.my_tiendita.bodega.BodegaService;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaDTO;
import io.bootify.my_tiendita.producto_bodega.ProductoBodegaService;
import io.bootify.my_tiendita.solicitud_bodeguero.SolicitudBodegueroDTO;
import io.bootify.my_tiendita.solicitud_bodeguero.SolicitudBodegueroService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final BodegaService bodegaService;
    private final SolicitudBodegueroService solicitudBodegueroService;
    // ✅ 1. Declaración del servicio
    private final ProductoBodegaService productoBodegaService;

    // ✅ 2. CONSTRUCTOR ACTUALIZADO (¡Esto es lo que suele fallar!)
    // Debes tener los 3 servicios aquí dentro
    public ClienteController(final BodegaService bodegaService,
                             final SolicitudBodegueroService solicitudBodegueroService,
                             final ProductoBodegaService productoBodegaService) {
        this.bodegaService = bodegaService;
        this.solicitudBodegueroService = solicitudBodegueroService;
        this.productoBodegaService = productoBodegaService;
    }

    @GetMapping
    public String inicio(Model model) {
        List<BodegaDTO> bodegas = bodegaService.findActivas();
        model.addAttribute("todasLasBodegas", bodegas);
        return "cliente/index";
    }

    @GetMapping("/explorar")
    public String explorar(Model model) {
        return "cliente/explorar";
    }

    // ✅ 3. Lógica del Catálogo
    @GetMapping("/bodega/{id}")
    public String catalogo(@PathVariable Long id, 
                           @RequestParam(required = false) Long categoria,
                           Model model) {
        
        // A. Cargar Bodega (Si falla lanza excepción 404, no 500)
        BodegaDTO bodega = bodegaService.get(id);
        model.addAttribute("bodega", bodega);

        // B. Cargar Productos
        List<ProductoBodegaDTO> productos;
        
        // Validación null safety para el servicio
        if (productoBodegaService == null) {
            throw new RuntimeException("Error de configuración: ProductoBodegaService no fue inyectado.");
        }

        if (categoria != null) {
            productos = productoBodegaService.findByCategoria(id, categoria);
            model.addAttribute("categoriaActiva", categoria);
        } else {
            productos = productoBodegaService.findActivosByBodega(id);
        }
        
        model.addAttribute("productos", productos);

        // C. Extraer Categorías (Versión segura contra nulos)
        Map<Long, String> categoriasDisponibles = new HashMap<>();
        
        if (productos != null) {
            productos.forEach(p -> {
                if (p.getCategoriaId() != null && p.getCategoriaNombre() != null) {
                    categoriasDisponibles.put(p.getCategoriaId(), p.getCategoriaNombre());
                }
            });
        }
        
        model.addAttribute("categorias", categoriasDisponibles);

        return "cliente/catalogo";
    }

    @GetMapping("/carrito")
    public String carrito(Model model) {
        return "cliente/carrito";
    }

    @GetMapping("/trabaja-con-nosotros")
    public String trabajaConNosotros(Model model) {
        if (!model.containsAttribute("solicitud")) {
            model.addAttribute("solicitud", new SolicitudBodegueroDTO());
        }
        return "cliente/trabaja-con-nosotros";
    }

    @PostMapping("/trabaja-con-nosotros")
    public String procesarSolicitud(@Valid @ModelAttribute("solicitud") SolicitudBodegueroDTO solicitudDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "cliente/trabaja-con-nosotros";
        }

        try {
            solicitudBodegueroService.create(solicitudDTO);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Solicitud enviada con éxito! Nos pondremos en contacto pronto.");
            return "redirect:/cliente/trabaja-con-nosotros";
            
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.email", e.getMessage());
            return "cliente/trabaja-con-nosotros";
        }
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(Model model) {
        return "cliente/mis-pedidos";
    }
}