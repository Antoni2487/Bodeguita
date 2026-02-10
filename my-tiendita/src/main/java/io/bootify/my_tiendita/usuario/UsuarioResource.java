package io.bootify.my_tiendita.usuario;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping(value = "/api/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioResource {

    private final UsuarioService usuarioService;

    public UsuarioResource(final UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ========== SOLO ADMIN ==========
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDTO>> getAllUsuarios() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> getUsuario(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(usuarioService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Map<String, Object>> createUsuario(@RequestBody @Valid final UsuarioDTO usuarioDTO) {
        try {
            // Validar si el email ya existe
            if (usuarioService.emailExists(usuarioDTO.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El email ya está registrado"
                ));
            }

            final Long createdId = usuarioService.create(usuarioDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Usuario creado correctamente",
                "id", createdId
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al crear usuario: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUsuario(
            @PathVariable(name = "id") final Long id,
            @RequestBody @Valid final UsuarioDTO usuarioDTO) {
        try {
            usuarioService.update(id, usuarioDTO);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario actualizado correctamente"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al actualizar: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Map<String, Object>> deleteUsuario(@PathVariable(name = "id") final Long id) {
        try {
            usuarioService.delete(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario eliminado correctamente"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al eliminar: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene la lista de usuarios con rol BODEGUERO
     * Se usa para el dropdown al crear bodegas
     */
    @GetMapping("/bodegueros")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDTO>> obtenerBodegueros() {
        return ResponseEntity.ok(usuarioService.obtenerBodegueros());
    }

    // ========== CUALQUIER USUARIO AUTENTICADO ==========
    
    @PostMapping("/actualizar-ubicacion")
    @PreAuthorize("isAuthenticated()")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<?> actualizarUbicacion(
            @RequestBody Map<String, Double> ubicacion,
            Authentication authentication) {
        
        try {
            // Obtener el email del usuario autenticado
            String email = authentication.getName();
            
            // Actualizar ubicación
            usuarioService.actualizarUbicacionPorEmail(
                email, 
                ubicacion.get("latitud"), 
                ubicacion.get("longitud")
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "mensaje", "Ubicación actualizada correctamente"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "success", false, 
                    "mensaje", "Error al actualizar ubicación: " + e.getMessage()
                ));
        }
    }
}