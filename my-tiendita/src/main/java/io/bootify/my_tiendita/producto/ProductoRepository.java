package io.bootify.my_tiendita.producto;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Producto findFirstByBodegaId(Long id);

}
