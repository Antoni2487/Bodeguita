package io.bootify.my_tiendita.pedido;

import io.bootify.my_tiendita.model.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // --- Validaciones de Integridad (usadas al borrar entidades padre) ---
    Pedido findFirstByUsuarioId(Long id);
    Pedido findFirstByBodegaId(Long id);
    Pedido findFirstByVentaId(Long id);

    // --- Solución al error del validador @PedidoVentaUnique ---
    boolean existsByVentaId(Long id);

    // --- Lógica de Negocio y Colas ---

    // 1. Recuperar TODOS los pendientes globales (para reiniciar la cola en memoria)
    List<Pedido> findByEstadoOrderByDateCreatedAsc(EstadoPedido estado);

    @Query("SELECT p FROM Pedido p " +
           "JOIN FETCH p.usuario " +
           "JOIN FETCH p.bodega " +
           "WHERE p.bodega.id = :bodegaId " +
           "ORDER BY p.dateCreated DESC")
    List<Pedido> findByBodegaIdOrderByDateCreatedDesc(@Param("bodegaId") Long bodegaId);

    // 3. (Opcional) Pendientes específicos de una bodega por si falla la cola en memoria
    List<Pedido> findByBodegaIdAndEstadoOrderByDateCreatedAsc(Long bodegaId, EstadoPedido estado);

    @Query("SELECT p FROM Pedido p " +
           "JOIN FETCH p.bodega " +       // Trae datos de la bodega
           "JOIN FETCH p.usuario " +      // ✅ Trae datos del usuario (ESTO FALTABA)
           "LEFT JOIN FETCH p.venta " +   // Trae venta si existe
           "WHERE p.usuario.id = :usuarioId " +
           "ORDER BY p.dateCreated DESC")
    List<Pedido> findByUsuarioIdOrderByDateCreatedDesc(@Param("usuarioId") Long usuarioId);

    @Query("SELECT p FROM Pedido p JOIN FETCH p.usuario JOIN FETCH p.bodega WHERE p.bodega.id = :bodegaId ORDER BY p.dateCreated DESC")
    List<Pedido> findHistorialConRelaciones(@Param("bodegaId") Long bodegaId);
}