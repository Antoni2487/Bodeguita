package io.bootify.my_tiendita.venta;

import io.bootify.my_tiendita.events.BeforeDeleteVenta;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ApplicationEventPublisher publisher;

    public VentaService(final VentaRepository ventaRepository,
            final ApplicationEventPublisher publisher) {
        this.ventaRepository = ventaRepository;
        this.publisher = publisher;
    }

    public List<VentaDTO> findAll() {
        final List<Venta> ventas = ventaRepository.findAll(Sort.by("id"));
        return ventas.stream()
                .map(venta -> mapToDTO(venta, new VentaDTO()))
                .toList();
    }

    public VentaDTO get(final Long id) {
        return ventaRepository.findById(id)
                .map(venta -> mapToDTO(venta, new VentaDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final VentaDTO ventaDTO) {
        final Venta venta = new Venta();
        mapToEntity(ventaDTO, venta);
        return ventaRepository.save(venta).getId();
    }

    public void update(final Long id, final VentaDTO ventaDTO) {
        final Venta venta = ventaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(ventaDTO, venta);
        ventaRepository.save(venta);
    }

    public void delete(final Long id) {
        final Venta venta = ventaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteVenta(id));
        ventaRepository.delete(venta);
    }

    private VentaDTO mapToDTO(final Venta venta, final VentaDTO ventaDTO) {
        ventaDTO.setId(venta.getId());
        ventaDTO.setMonto(venta.getMonto());
        ventaDTO.setFecha(venta.getFecha());
        return ventaDTO;
    }

    private Venta mapToEntity(final VentaDTO ventaDTO, final Venta venta) {
        venta.setMonto(ventaDTO.getMonto());
        venta.setFecha(ventaDTO.getFecha());
        return venta;
    }

    public Map<Long, Long> getVentaValues() {
        return ventaRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Venta::getId, Venta::getId));
    }

}
