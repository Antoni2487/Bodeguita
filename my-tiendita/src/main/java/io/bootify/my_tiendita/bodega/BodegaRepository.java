package io.bootify.my_tiendita.bodega;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


public interface BodegaRepository extends JpaRepository<Bodega, Long> {

    List<Bodega> findAllByActivoTrue();

    List<Bodega> findByDistritoIgnoreCase(String distrito);
    

}
