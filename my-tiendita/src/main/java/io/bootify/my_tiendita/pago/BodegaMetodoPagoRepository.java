package io.bootify.my_tiendita.pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BodegaMetodoPagoRepository extends JpaRepository<BodegaMetodoPago, Long> {
    List<BodegaMetodoPago> findByBodegaId(Long bodegaId);

    boolean existsByBodegaIdAndTipoMetodoPagoId(Long id, Long id2);
}