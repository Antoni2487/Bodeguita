package io.bootify.my_tiendita.admin; // O el paquete donde tengas tus controllers admin

import io.bootify.my_tiendita.notificacion.NotificacionRepository;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "io.bootify.my_tiendita.admin") // Solo afecta al paquete admin
public class AdminGlobalControllerAdvice {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminGlobalControllerAdvice(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("notificacionesNoLeidasCount")
    public long addNotificacionesCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            // Buscamos al usuario logueado para sacar su ID
            return usuarioRepository.findByEmail(email)
                    .map(usuario -> notificacionRepository.countByUsuarioDestinoIdAndLeidoFalse(usuario.getId()))
                    .orElse(0L);
        }
        return 0L;
    }
}