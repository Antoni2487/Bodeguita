package io.bootify.my_tiendita.venta;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/ventas", produces = MediaType.APPLICATION_JSON_VALUE)
public class VentaResource {

    private final VentaService ventaService;

    public VentaResource(final VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public ResponseEntity<List<VentaDTO>> getAllVentas() {
        return ResponseEntity.ok(ventaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaDTO> getVenta(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(ventaService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createVenta(@RequestBody @Valid final VentaDTO ventaDTO) {
        final Long createdId = ventaService.create(ventaDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<Void> anularVenta(@PathVariable(name = "id") final Long id) {
        ventaService.anular(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateVenta(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final VentaDTO ventaDTO) {
        ventaService.update(id, ventaDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteVenta(@PathVariable(name = "id") final Long id) {
        ventaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}