package io.bootify.my_tiendita.tienda;

import io.bootify.my_tiendita.pedido.PedidoDTO;
import io.bootify.my_tiendita.pedido.PedidoService;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/tienda")
public class TiendaRestController {

    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;

    public TiendaRestController(PedidoService pedidoService, UsuarioRepository usuarioRepository) {
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/pedido")
    public ResponseEntity<Map<String, Long>> crearPedido(@RequestBody @Valid PedidoDTO pedidoDTO) {
        
        // 1. Obtener el Usuario Autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        if (email == null || email.equals("anonymousUser")) {
             throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para comprar");
        }

        // ✅ CORRECCIÓN: Manejo del Optional
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado en base de datos"));

        // 2. Inyectar el ID del usuario seguro
        pedidoDTO.setUsuario(cliente.getId());

        // 3. Crear el Pedido
        Long pedidoId = pedidoService.create(pedidoDTO);

        return ResponseEntity.ok(Collections.singletonMap("pedidoId", pedidoId));
    }
}