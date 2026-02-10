package io.bootify.my_tiendita.admin;

import io.bootify.my_tiendita.usuario.UsuarioService;
import io.bootify.my_tiendita.bodega.BodegaService;
import io.bootify.my_tiendita.producto.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')") 
public class AdminController {

    private final UsuarioService usuarioService;
    private final BodegaService bodegaService;
    private final ProductoService productoService;

    public AdminController(
            final UsuarioService usuarioService,
            final BodegaService bodegaService,
            final ProductoService productoService) {
        this.usuarioService = usuarioService;
        this.bodegaService = bodegaService;
        this.productoService = productoService;
    }

    // ========== VISTA: DASHBOARD ==========
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {

        long totalUsuarios = usuarioService.countAll();
        long usuariosActivos = usuarioService.countActivos();
        long totalBodegas = bodegaService.countAll();
        long totalProductos = productoService.countAll();

        String nombreAdmin = authentication.getName();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("totalBodegas", totalBodegas);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("nombreAdmin", nombreAdmin);

        return "admin/dashboard";
    }

    // ========== VISTA: GESTIÓN DE USUARIOS ==========
    @GetMapping("/usuarios")
    public String gestionUsuarios() {
        // La tabla se llena por AJAX (desde /api/usuarios)
        return "admin/usuarios";
    }

    // ========== VISTA: GESTIÓN DE BODEGAS ==========
    @GetMapping("/bodegas")
    public String gestionBodegas() {
        // La tabla se llena por AJAX (desde /api/bodegas)
        return "admin/bodegas";
    }

    // ========== VISTA: GESTIÓN DE CATEGORÍAS ==========
    @GetMapping("/categorias")
    public String gestionCategorias() {
        return "admin/categorias";
    }

    @GetMapping("/productos")
    public String gestionProductos() {
        return "admin/productos";
    }

    @GetMapping("/solicitudes")
    public String gestionSolicitudes() {
        return "admin/solicitudes-bodegueros";
    }
    
}
