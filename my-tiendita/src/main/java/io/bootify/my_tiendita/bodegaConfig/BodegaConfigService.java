package io.bootify.my_tiendita.bodegaConfig;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BodegaConfigService {

    private final BodegaConfigRepository bodegaConfigRepository;
    private final BodegaRepository bodegaRepository;

    public BodegaConfigService(BodegaConfigRepository bodegaConfigRepository, BodegaRepository bodegaRepository) {
        this.bodegaConfigRepository = bodegaConfigRepository;
        this.bodegaRepository = bodegaRepository;
    }

    public BodegaConfigDTO obtenerConfiguracion(Long bodegaId) {
        // Buscamos la bodega (siempre existe)
        Bodega bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));

        // Buscamos su config (puede no existir aún)
        BodegaConfig config = bodegaConfigRepository.findByBodegaId(bodegaId)
                .orElse(new BodegaConfig());

        return mapToDTO(bodega, config);
    }

    @Transactional
    public void guardarConfiguracion(Long bodegaId, BodegaConfigDTO dto) {
        // 1. ACTUALIZAR TABLA BODEGA (Datos básicos y ubicación)
        Bodega bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));

        bodega.setNombre(dto.getNombre());
        bodega.setDireccion(dto.getDireccion());
        bodega.setTelefono(dto.getTelefono());
        bodega.setHorario(dto.getHorario());
        bodega.setLatitud(dto.getLatitud());
        bodega.setLongitud(dto.getLongitud());
        
        bodegaRepository.save(bodega);

        BodegaConfig config = bodegaConfigRepository.findByBodegaId(bodegaId)
                .orElse(new BodegaConfig());
        
        if (config.getBodega() == null) {
            config.setBodega(bodega);
        }

        config.setRealizaDelivery(dto.getRealizaDelivery());
        config.setRadioMaximoKm(dto.getRadioMaximoKm());
        config.setPrecioPorKm(dto.getPrecioPorKm());
        config.setPedidoMinimoDelivery(dto.getPedidoMinimoDelivery());

        bodegaConfigRepository.save(config);
    }

    // Unificamos datos de DOS entidades en UN DTO
    private BodegaConfigDTO mapToDTO(Bodega bodega, BodegaConfig config) {
        BodegaConfigDTO dto = new BodegaConfigDTO();
        
        // De Bodega
        dto.setNombre(bodega.getNombre());
        dto.setDireccion(bodega.getDireccion());
        dto.setTelefono(bodega.getTelefono());
        dto.setHorario(bodega.getHorario());
        dto.setLatitud(bodega.getLatitud());
        dto.setLongitud(bodega.getLongitud());

        // De Config
        dto.setId(config.getId());
        dto.setRealizaDelivery(config.getRealizaDelivery() != null ? config.getRealizaDelivery() : false);
        dto.setRadioMaximoKm(config.getRadioMaximoKm());
        dto.setPrecioPorKm(config.getPrecioPorKm());
        dto.setPedidoMinimoDelivery(config.getPedidoMinimoDelivery());
        
        return dto;
    }
}