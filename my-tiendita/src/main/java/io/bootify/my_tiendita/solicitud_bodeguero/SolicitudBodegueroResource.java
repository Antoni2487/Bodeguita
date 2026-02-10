package io.bootify.my_tiendita.solicitud_bodeguero;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/solicitudes", produces = MediaType.APPLICATION_JSON_VALUE)
public class SolicitudBodegueroResource {

    private final SolicitudBodegueroService solicitudBodegueroService;

    public SolicitudBodegueroResource(final SolicitudBodegueroService solicitudBodegueroService) {
        this.solicitudBodegueroService = solicitudBodegueroService;
    }

    // 1. GET: Listar todas
    @GetMapping
    public ResponseEntity<List<SolicitudBodegueroDTO>> getAllSolicitudes() {
        return ResponseEntity.ok(solicitudBodegueroService.findAll());
    }

    // 2. GET: Una sola (por si acaso)
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudBodegueroDTO> getSolicitud(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(solicitudBodegueroService.get(id));
    }

    // 3. POST: Crear (usado por el formulario público si quisieras hacerlo vía AJAX, aunque ya lo tienes por MVC)
    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createSolicitud(@RequestBody @Valid final SolicitudBodegueroDTO solicitudBodegueroDTO) {
        final Long createdId = solicitudBodegueroService.create(solicitudBodegueroDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    // 4. POST: APROBAR
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<Void> aprobarSolicitud(@PathVariable(name = "id") final Long id) {
        solicitudBodegueroService.aprobarSolicitud(id);
        return ResponseEntity.ok().build();
    }

    // 5. POST: RECHAZAR
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazarSolicitud(@PathVariable(name = "id") final Long id) {
        solicitudBodegueroService.rechazarSolicitud(id);
        return ResponseEntity.ok().build();
    }

    // 6. DELETE
    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteSolicitud(@PathVariable(name = "id") final Long id) {
        solicitudBodegueroService.delete(id);
        return ResponseEntity.noContent().build();
    }
}