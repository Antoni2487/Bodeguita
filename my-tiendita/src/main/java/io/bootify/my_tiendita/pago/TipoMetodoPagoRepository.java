package io.bootify.my_tiendita.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoMetodoPagoRepository extends JpaRepository<TipoMetodoPago, Long> {
    
    
    Optional<TipoMetodoPago> findByNombreIgnoreCase(String nombre);
}