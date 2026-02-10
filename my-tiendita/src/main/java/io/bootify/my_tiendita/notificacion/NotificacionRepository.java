package io.bootify.my_tiendita.notificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioDestinoIdOrderByDateCreatedDesc(Long usuarioId);

    long countByUsuarioDestinoIdAndLeidoFalse(Long usuarioId);

    List<Notificacion> findAllByOrderByDateCreatedAsc();
}