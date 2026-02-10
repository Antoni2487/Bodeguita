package io.bootify.my_tiendita.pedido;

import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger; // Logger
import org.slf4j.LoggerFactory; // Logger
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/cliente/pedidos", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "bearer-jwt") 
public class PedidoResource {

    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    // ✅ LOGGER PARA VER EL ERROR REAL EN CONSOLA
    private static final Logger logger = LoggerFactory.getLogger(PedidoResource.class);

    public PedidoResource(final PedidoService pedidoService, UsuarioRepository usuarioRepository) {
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/pre-checkout")
    public ResponseEntity<CheckoutResponseDTO> preCheckout(@RequestBody @Valid CheckoutRequestDTO request) {
        // Log para depurar si llegan los datos
        logger.info("Pre-checkout solicitado para bodega: {}", request.getBodegaId());
        CheckoutResponseDTO response = pedidoService.validarPreCheckout(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<?> createPedido( // Usamos wildcard <?> para poder devolver errores de texto si falla
            @RequestBody @Valid PedidoDTO pedidoDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            logger.info("Iniciando creación de pedido...");

            if (userDetails == null) {
                logger.error("Usuario no autenticado (userDetails es null)");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Debes iniciar sesión para pedir.");
            }

            String email = userDetails.getUsername();
            logger.info("Usuario autenticado: " + email);

            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("El usuario con email " + email + " no existe en la BD."));
            
            pedidoDTO.setUsuario(usuario.getId());

            final Long createdId = pedidoService.create(pedidoDTO);
            logger.info("Pedido creado con ID: " + createdId);
            
            return new ResponseEntity<>(createdId, HttpStatus.CREATED);

        } catch (Exception e) {
            // 🔥 AQUÍ VERÁS EL ERROR REAL EN TU CONSOLA DE JAVA (IntelliJ/Eclipse)
            logger.error("ERROR GRAVE CREANDO PEDIDO: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en el servidor: " + e.getMessage());
        }
    }

   @GetMapping
    public ResponseEntity<List<PedidoDTO>> getAllPedidosCliente(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

    
        return ResponseEntity.ok(pedidoService.findAllByUsuario(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> getPedido(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(pedidoService.get(id));
    }
}