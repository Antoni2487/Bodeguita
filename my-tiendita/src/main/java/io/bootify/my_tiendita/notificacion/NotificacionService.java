package io.bootify.my_tiendita.notificacion;

import io.bootify.my_tiendita.estructuras.GestorPilasNotificaciones; // ✅ Importar
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    
    private final GestorPilasNotificaciones gestorPilas;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository,
                               GestorPilasNotificaciones gestorPilas) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.gestorPilas = gestorPilas;
    }


    @PostConstruct
    public void inicializarPilas() {
        List<Notificacion> todas = notificacionRepository.findAllByOrderByDateCreatedAsc();
        for (Notificacion n : todas) {
            gestorPilas.apilar(n.getUsuarioDestino().getId(), n);
        }
        System.out.println("✅ Estructura LIFO inicializada.");
    }

    @Transactional
    public void notificar(Long usuarioDestinoId, String mensaje, String tipo, String url) {
        Usuario usuario = usuarioRepository.findById(usuarioDestinoId).orElseThrow();

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioDestino(usuario);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo);
        notificacion.setUrlDestino(url);
        notificacion.setLeido(false);
        notificacion.setDateCreated(OffsetDateTime.now());

        Notificacion guardada = notificacionRepository.save(notificacion); 

        gestorPilas.apilar(usuarioDestinoId, guardada);
    }

    public List<Notificacion> obtenerMisNotificaciones(Long usuarioId) {
        return gestorPilas.obtenerPilaComoLista(usuarioId);
    }
    
    
}