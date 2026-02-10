package io.bootify.my_tiendita.venta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT DISTINCT v FROM Venta v " +
           "LEFT JOIN FETCH v.tipoMetodoPago " +
           "LEFT JOIN FETCH v.detalles d " +
           "LEFT JOIN FETCH d.productoBodega pb " +
           "LEFT JOIN FETCH pb.producto p " + 
           "WHERE pb.bodega.id = :bodegaId " +
           "ORDER BY v.fecha DESC")
    List<Venta> findByBodegaId(@Param("bodegaId") Long bodegaId);
}
