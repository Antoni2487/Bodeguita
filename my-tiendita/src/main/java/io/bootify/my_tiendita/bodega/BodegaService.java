package io.bootify.my_tiendita.bodega;

import io.bootify.my_tiendita.events.BeforeDeleteBodega;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class BodegaService {

    private final BodegaRepository bodegaRepository;
    private final ApplicationEventPublisher publisher;

    public BodegaService(final BodegaRepository bodegaRepository,
            final ApplicationEventPublisher publisher) {
        this.bodegaRepository = bodegaRepository;
        this.publisher = publisher;
    }

    public List<BodegaDTO> findAll() {
        final List<Bodega> bodegas = bodegaRepository.findAll(Sort.by("id"));
        return bodegas.stream()
                .map(bodega -> mapToDTO(bodega, new BodegaDTO()))
                .toList();
    }

    public BodegaDTO get(final Long id) {
        return bodegaRepository.findById(id)
                .map(bodega -> mapToDTO(bodega, new BodegaDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final BodegaDTO bodegaDTO) {
        final Bodega bodega = new Bodega();
        mapToEntity(bodegaDTO, bodega);
        return bodegaRepository.save(bodega).getId();
    }

    public void update(final Long id, final BodegaDTO bodegaDTO) {
        final Bodega bodega = bodegaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(bodegaDTO, bodega);
        bodegaRepository.save(bodega);
    }

    public void delete(final Long id) {
        final Bodega bodega = bodegaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteBodega(id));
        bodegaRepository.delete(bodega);
    }

    private BodegaDTO mapToDTO(final Bodega bodega, final BodegaDTO bodegaDTO) {
        bodegaDTO.setId(bodega.getId());
        bodegaDTO.setNombre(bodega.getNombre());
        bodegaDTO.setDireccion(bodega.getDireccion());
        bodegaDTO.setLatitud(bodega.getLatitud());
        bodegaDTO.setLongitud(bodega.getLongitud());
        bodegaDTO.setActivo(bodega.getActivo());
        return bodegaDTO;
    }

    private Bodega mapToEntity(final BodegaDTO bodegaDTO, final Bodega bodega) {
        bodega.setNombre(bodegaDTO.getNombre());
        bodega.setDireccion(bodegaDTO.getDireccion());
        bodega.setLatitud(bodegaDTO.getLatitud());
        bodega.setLongitud(bodegaDTO.getLongitud());
        bodega.setActivo(bodegaDTO.getActivo());
        return bodega;
    }

    public Map<Long, String> getBodegaValues() {
        return bodegaRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Bodega::getId, Bodega::getNombre));
    }

}
