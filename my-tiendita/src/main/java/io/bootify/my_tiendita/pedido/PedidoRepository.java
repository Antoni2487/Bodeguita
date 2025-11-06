package io.bootify.my_tiendita.pedido;

import org.springframework.data.jpa.repository.JpaRepository;


public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Pedido findFirstByUsuarioId(Long id);

    Pedido findFirstByBodegaId(Long id);

    Pedido findFirstByVentaId(Long id);

    boolean existsByVentaId(Long id);

}
