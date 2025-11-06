package io.bootify.my_tiendita.detalle_pedido;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/detallePedidos", produces = MediaType.APPLICATION_JSON_VALUE)
public class DetallePedidoResource {

    private final DetallePedidoService detallePedidoService;

    public DetallePedidoResource(final DetallePedidoService detallePedidoService) {
        this.detallePedidoService = detallePedidoService;
    }

    @GetMapping
    public ResponseEntity<List<DetallePedidoDTO>> getAllDetallePedidos() {
        return ResponseEntity.ok(detallePedidoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetallePedidoDTO> getDetallePedido(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(detallePedidoService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createDetallePedido(
            @RequestBody @Valid final DetallePedidoDTO detallePedidoDTO) {
        final Long createdId = detallePedidoService.create(detallePedidoDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateDetallePedido(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final DetallePedidoDTO detallePedidoDTO) {
        detallePedidoService.update(id, detallePedidoDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteDetallePedido(@PathVariable(name = "id") final Long id) {
        detallePedidoService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
