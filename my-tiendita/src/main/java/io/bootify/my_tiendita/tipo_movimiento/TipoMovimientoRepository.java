package io.bootify.my_tiendita.tipo_movimiento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Long> {
    
    java.util.Optional<TipoMovimiento> findByNombre(String nombre);

}
