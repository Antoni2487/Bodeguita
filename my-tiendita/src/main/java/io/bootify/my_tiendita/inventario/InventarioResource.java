package io.bootify.my_tiendita.inventario;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioResource {

    private final InventarioService service;

    public InventarioResource(InventarioService service) {
        this.service = service;
    }

    @GetMapping("/{productoBodegaId}")
    public ResponseEntity<List<InventarioDTO>> obtenerHistorial(
            @PathVariable Long productoBodegaId) {
        return ResponseEntity.ok(service.obtenerHistorial(productoBodegaId));
    }

    @PostMapping
    public ResponseEntity<Long> registrarMovimiento(
            @RequestBody InventarioDTO dto) {
        return ResponseEntity.ok(service.registrarMovimiento(dto));
    }

    
}
