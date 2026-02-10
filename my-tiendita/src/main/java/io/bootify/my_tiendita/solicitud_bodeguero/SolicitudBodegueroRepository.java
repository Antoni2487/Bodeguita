package io.bootify.my_tiendita.solicitud_bodeguero;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitudBodegueroRepository extends JpaRepository<SolicitudBodeguero, Long> {

    boolean existsByEmailIgnoreCase(String email);
    
    boolean existsByNombreBodegaIgnoreCase(String nombreBodega);

    List<SolicitudBodeguero> findByEstado(String estado);
}