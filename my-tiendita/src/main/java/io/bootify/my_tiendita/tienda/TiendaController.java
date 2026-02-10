package io.bootify.my_tiendita.tienda;

import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tienda")
public class TiendaController {

    private final TiendaService tiendaService;
    private final BodegaRepository bodegaRepository;

    public TiendaController(TiendaService tiendaService, BodegaRepository bodegaRepository) {
        this.tiendaService = tiendaService;
        this.bodegaRepository = bodegaRepository;
    }

    /**
     * Home de la tienda (Catálogo)
     * URL: /tienda/{bodegaId}
     */
    @GetMapping("/{bodegaId}")
    public String catalogo(@PathVariable Long bodegaId, 
                           @RequestParam(required = false) Long categoria,
                           Model model) {
        
        // 1. Validar Bodega y pasar info básica al header
        var bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        model.addAttribute("bodega", bodega);

        // 2. Cargar Categorías para el filtro
        model.addAttribute("categorias", tiendaService.obtenerCategoriasConProductos(bodegaId));

        // 3. Cargar Productos (Filtrados o Todos)
        if (categoria != null) {
            model.addAttribute("productos", tiendaService.obtenerPorCategoria(bodegaId, categoria));
            model.addAttribute("categoriaActiva", categoria);
        } else {
            model.addAttribute("productos", tiendaService.obtenerCatalogo(bodegaId));
        }

        return "cliente/catalogo"; // Vista que crearemos en el paso 5
    }

    /**
     * Detalle del Producto
     * URL: /tienda/{bodegaId}/producto/{productoId}
     */
    @GetMapping("/{bodegaId}/producto/{productoId}")
    public String detalleProducto(@PathVariable Long bodegaId,
                                  @PathVariable Long productoId,
                                  Model model) {
        
        var bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        model.addAttribute("bodega", bodega);

        model.addAttribute("producto", tiendaService.obtenerDetalleProducto(productoId));
        
        return "cliente/detalle"; // Vista que crearemos en el paso 5
    }

    @GetMapping("/{bodegaId}/carrito")
    public String verCarrito(@PathVariable Long bodegaId, Model model) {
        var bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        model.addAttribute("bodega", bodega);
        
        return "cliente/carrito";
    }
}