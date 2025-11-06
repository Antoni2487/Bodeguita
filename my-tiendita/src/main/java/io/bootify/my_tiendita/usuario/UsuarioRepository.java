package io.bootify.my_tiendita.usuario;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findFirstByBodegasId(Long id);

    boolean existsByEmailIgnoreCase(String email);

}
