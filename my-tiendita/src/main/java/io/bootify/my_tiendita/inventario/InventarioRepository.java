package io.bootify.my_tiendita.inventario;

import io.bootify.my_tiendita.producto_bodega.ProductoBodega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    List<Inventario> findByProductoBodegaOrderByFechaDesc(ProductoBodega productoBodega);
}
