package io.bootify.my_tiendita.bodega;

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
@RequestMapping(value = "/api/bodegas", produces = MediaType.APPLICATION_JSON_VALUE)
public class BodegaResource {

    private final BodegaService bodegaService;

    public BodegaResource(final BodegaService bodegaService) {
        this.bodegaService = bodegaService;
    }

    @GetMapping
    public ResponseEntity<List<BodegaDTO>> getAllBodegas() {
        return ResponseEntity.ok(bodegaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BodegaDTO> getBodega(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(bodegaService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createBodega(@RequestBody @Valid final BodegaDTO bodegaDTO) {
        final Long createdId = bodegaService.create(bodegaDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateBodega(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final BodegaDTO bodegaDTO) {
        bodegaService.update(id, bodegaDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteBodega(@PathVariable(name = "id") final Long id) {
        bodegaService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
