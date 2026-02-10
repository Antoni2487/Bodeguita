package io.bootify.my_tiendita.usuario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findFirstByBodegas_Id(Long bodegaId);

    boolean existsByBodegas_Id(Long bodegaId);

    long countByActivoTrue();

    long countDistinctByBodegas_Id(Long bodegaId);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByEmail(String email);

    boolean existsByNumeroDocumento(String numeroDocumento);

    List<Usuario> findByRoles_Nombre(String nombre, Sort sort);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.bodegas WHERE u.email = :email")
    Optional<Usuario> findByEmailWithBodegas(@Param("email") String email);
}
