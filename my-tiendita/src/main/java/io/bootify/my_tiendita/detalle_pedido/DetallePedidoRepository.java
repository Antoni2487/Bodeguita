package io.bootify.my_tiendita.detalle_pedido;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {


    DetallePedido findFirstByPedidoId(Long id);

    DetallePedido findFirstByProductoBodegaId(Long id);
    

}