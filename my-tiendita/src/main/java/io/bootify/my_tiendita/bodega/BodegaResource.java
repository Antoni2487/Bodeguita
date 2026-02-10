package io.bootify.my_tiendita.bodega;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API REST para gestión de Bodegas (Admin y Bodeguero)
 */
@RestController
@RequestMapping(value = "/api/bodegas", produces = MediaType.APPLICATION_JSON_VALUE)
public class BodegaResource {

    private final BodegaService bodegaService;

    public BodegaResource(final BodegaService bodegaService) {
        this.bodegaService = bodegaService;
    }

    // ========== ADMIN ==========

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllBodegas() {
        try {
            List<BodegaDTO> bodegas = bodegaService.findAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", bodegas,
                "message", "Listado de bodegas obtenido correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al obtener bodegas: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getBodega(@PathVariable final Long id) {
        try {
            BodegaDTO bodega = bodegaService.get(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", bodega,
                "message", "Bodega obtenida correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "No se encontró la bodega con ID " + id
            ));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Map<String, Object>> createBodega(@RequestBody @Valid final BodegaDTO bodegaDTO) {
        try {
            final Long createdId = bodegaService.create(bodegaDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Bodega creada correctamente",
                "id", createdId
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al crear la bodega: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateBodega(
            @PathVariable final Long id,
            @RequestBody @Valid final BodegaDTO bodegaDTO) {
        try {
            bodegaService.update(id, bodegaDTO);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bodega actualizada correctamente"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al actualizar la bodega: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Map<String, Object>> deleteBodega(@PathVariable final Long id) {
        try {
            bodegaService.delete(id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bodega eliminada correctamente"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al eliminar la bodega: " + e.getMessage()
            ));
        }
    }

    // ========== ADMIN y BODEGUERO ==========

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMIN','BODEGUERO')")
    public ResponseEntity<Map<String, Object>> getBodegasActivas() {
        try {
            List<BodegaDTO> bodegas = bodegaService.findAll()
                    .stream()
                    .filter(BodegaDTO::getActivo)
                    .toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", bodegas,
                "message", "Bodegas activas obtenidas correctamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al obtener bodegas activas: " + e.getMessage()
            ));
        }
    }


@PostMapping("/crear-con-bodeguero")
@PreAuthorize("hasRole('ADMIN')")
@ApiResponse(responseCode = "201")
public ResponseEntity<Map<String, Object>> crearBodegaConBodeguero(
        @RequestBody @Valid final BodegaConBodegueroDTO dto) {
    try {
        Long bodegaId = bodegaService.crearBodegaConBodeguero(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "message", "Bodega creada y asociada con bodeguero correctamente",
            "id", bodegaId
        ));
        
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", e.getMessage()
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "Error al crear la bodega: " + e.getMessage()
        ));
    }
}

   
    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerDetalleBodega(@PathVariable final Long id) {
        try {
            Map<String, Object> detalle = bodegaService.obtenerDetalleBodega(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", detalle,
                "message", "Detalle de bodega obtenido correctamente"
            ));
            
        } catch (io.bootify.my_tiendita.util.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Bodega no encontrada con ID " + id
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error al obtener detalle: " + e.getMessage()
            ));
        }
    }

    /**
     * Asigna un bodeguero adicional a una bodega existente
     * 
     * @param bodegaId ID de la bodega
     * @param usuarioId ID del usuario (bodeguero)
     * @return Respuesta de éxito o error
     */
    @PostMapping("/{bodegaId}/asignar-bodeguero/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> asignarBodeguero(
            @PathVariable final Long bodegaId,
            @PathVariable final Long usuarioId) {
        try {
            bodegaService.asignarBodegueroAdicional(bodegaId, usuarioId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bodeguero asignado correctamente a la bodega"
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
            
        } catch (io.bootify.my_tiendita.util.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error al asignar bodeguero: " + e.getMessage()
            ));
        }
    }
}
