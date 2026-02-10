package io.bootify.my_tiendita.tipo_movimiento;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tipos-movimiento")
public class TipoMovimientoResource {

    private final TipoMovimientoRepository tipoMovimientoRepository;

    public TipoMovimientoResource(TipoMovimientoRepository tipoMovimientoRepository) {
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }

    @GetMapping
    public ResponseEntity<List<TipoMovimiento>> listarTodos() {
        return ResponseEntity.ok(tipoMovimientoRepository.findAll());
    }
}
