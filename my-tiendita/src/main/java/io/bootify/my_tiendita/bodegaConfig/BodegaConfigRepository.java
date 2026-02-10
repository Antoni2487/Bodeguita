package io.bootify.my_tiendita.bodegaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BodegaConfigRepository extends JpaRepository<BodegaConfig, Long> {
    Optional<BodegaConfig> findByBodegaId(Long bodegaId);
}