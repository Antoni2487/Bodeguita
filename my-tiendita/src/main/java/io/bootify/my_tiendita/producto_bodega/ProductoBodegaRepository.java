package io.bootify.my_tiendita.producto_bodega;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.producto.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface ProductoBodegaRepository extends JpaRepository<ProductoBodega, Long> {

   
    List<ProductoBodega> findByBodegaId(Long bodegaId);


    List<ProductoBodega> findByBodegaIdAndActivoTrue(Long bodegaId);


    List<ProductoBodega> findByProductoId(Long productoId);

    
    Optional<ProductoBodega> findByProductoIdAndBodegaId(Long productoId, Long bodegaId);


    boolean existsByProductoIdAndBodegaId(Long productoId, Long bodegaId);

    @Query("SELECT pb FROM ProductoBodega pb " +
           "JOIN FETCH pb.producto p " +
           "JOIN FETCH p.categoria " +
           "WHERE pb.bodega.id = :bodegaId " +
           "ORDER BY p.nombre ASC")
    List<ProductoBodega> findInventarioCompletoPorBodega(@Param("bodegaId") Long bodegaId);

    @Query("SELECT pb FROM ProductoBodega pb WHERE pb.bodega.id = :bodegaId AND pb.producto.categoria.id = :categoriaId")
    List<ProductoBodega> findByBodegaIdAndCategoriaId(@Param("bodegaId") Long bodegaId, 
                                                       @Param("categoriaId") Long categoriaId);

    @Query("SELECT pb FROM ProductoBodega pb WHERE pb.bodega.id = :bodegaId " +
           "AND pb.producto.categoria.id = :categoriaId AND pb.activo = true")
    List<ProductoBodega> findByBodegaIdAndCategoriaIdAndActivoTrue(@Param("bodegaId") Long bodegaId, 
                                                                     @Param("categoriaId") Long categoriaId);

    long countByBodegaIdAndActivoTrue(Long bodegaId);

    @Query("SELECT pb FROM ProductoBodega pb WHERE pb.bodega.id = :bodegaId " +
           "AND LOWER(pb.producto.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<ProductoBodega> searchByBodegaIdAndProductoNombre(@Param("bodegaId") Long bodegaId, 
                                                            @Param("nombre") String nombre);
    ProductoBodega findFirstByProducto(Producto producto);

    ProductoBodega findFirstByBodega(Bodega bodega);

    @Query("SELECT pb FROM ProductoBodega pb " +
           "JOIN FETCH pb.producto p " +
           "LEFT JOIN FETCH p.categoria " +
           "WHERE pb.bodega.id = :bodegaId AND pb.activo = true " +
           "ORDER BY p.nombre ASC")
    List<ProductoBodega> findProductosParaVenta(@Param("bodegaId") Long bodegaId);

    @Query("SELECT pb FROM ProductoBodega pb " +
           "JOIN FETCH pb.producto p " +
           "JOIN FETCH p.categoria c " +
           "WHERE pb.bodega.id = :bodegaId " +
           "AND pb.activo = true " +
           "AND pb.stock > 0 " +
           "ORDER BY p.categoria.nombre ASC, p.nombre ASC")
    List<ProductoBodega> findProductosDisponiblesPorBodega(@Param("bodegaId") Long bodegaId);
    

    @Query("SELECT pb FROM ProductoBodega pb " +
           "JOIN FETCH pb.producto p " +
           "WHERE pb.bodega.id = :bodegaId " +
           "AND p.categoria.id = :categoriaId " +
           "AND pb.activo = true " +
           "AND pb.stock > 0")
    List<ProductoBodega> findProductosDisponiblesPorCategoria(@Param("bodegaId") Long bodegaId, 
                                                              @Param("categoriaId") Long categoriaId);


}