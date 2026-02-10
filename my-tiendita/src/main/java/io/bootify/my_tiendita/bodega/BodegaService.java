package io.bootify.my_tiendita.bodega;

import io.bootify.my_tiendita.events.BeforeDeleteBodega;
import io.bootify.my_tiendita.pago.BodegaMetodoPago;
import io.bootify.my_tiendita.pago.BodegaMetodoPagoRepository;
import io.bootify.my_tiendita.pago.TipoMetodoPagoRepository;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.bootify.my_tiendita.usuario.UsuarioService;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BodegaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BodegaService.class);

    private final BodegaRepository bodegaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher publisher;
    private final UsuarioService usuarioService;
    
    // ✅ Repositorios necesarios para inicializar pagos
    private final BodegaMetodoPagoRepository bodegaMetodoPagoRepository;
    private final TipoMetodoPagoRepository tipoMetodoPagoRepository;

    public BodegaService(final BodegaRepository bodegaRepository,
                         final UsuarioRepository usuarioRepository,
                         final ApplicationEventPublisher publisher,
                         final UsuarioService usuarioService,
                         final BodegaMetodoPagoRepository bodegaMetodoPagoRepository,
                         final TipoMetodoPagoRepository tipoMetodoPagoRepository) {
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
        this.publisher = publisher;
        this.usuarioService = usuarioService;
        this.bodegaMetodoPagoRepository = bodegaMetodoPagoRepository;
        this.tipoMetodoPagoRepository = tipoMetodoPagoRepository;
    }

    // ======================================
    // CONSULTAS
    // ======================================

    @Transactional(readOnly = true)
    public List<BodegaDTO> findActivas() {
        return bodegaRepository.findAllByActivoTrue().stream()
                .map(bodega -> mapToDTO(bodega, new BodegaDTO()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BodegaDTO> findAll() {
        return bodegaRepository.findAll(Sort.by("id")).stream()
                .map(bodega -> mapToDTO(bodega, new BodegaDTO()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BodegaDTO get(Long id) {
        return bodegaRepository.findById(id)
                .map(bodega -> mapToDTO(bodega, new BodegaDTO()))
                .orElseThrow(NotFoundException::new);
    }

    // ======================================
    // CREAR / ACTUALIZAR
    // ======================================

    @Transactional
    public Long create(final BodegaDTO bodegaDTO) {
        final Bodega bodega = new Bodega();
        mapToEntity(bodegaDTO, bodega);

        if (bodega.getActivo() == null) bodega.setActivo(true);
        if (bodega.getLatitud() == null) bodega.setLatitud(-12.0464);
        if (bodega.getLongitud() == null) bodega.setLongitud(-77.0428);

        Bodega bodegaGuardada = bodegaRepository.save(bodega);
        
        // ✅ Inicializar métodos de pago por defecto
        inicializarMetodosPago(bodegaGuardada);

        return bodegaGuardada.getId();
    }

    @Transactional
    public void update(final Long id, final BodegaDTO bodegaDTO) {
        final Bodega bodega = bodegaRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        
        // Mapea campos simples (nombre, dirección, etc.)
        mapToEntity(bodegaDTO, bodega);

        // ✅ LÓGICA AGREGADA: ACTUALIZAR EL DUEÑO (BODEGUERO)
        if (bodegaDTO.getUsuarioId() != null) {
            // Solo procesar si la bodega no tiene dueño O si el dueño es diferente al actual
            boolean cambioDueño = bodega.getUsuario() == null || !bodega.getUsuario().getId().equals(bodegaDTO.getUsuarioId());
            
            if (cambioDueño) {
                Usuario nuevoDueño = usuarioRepository.findById(bodegaDTO.getUsuarioId())
                        .orElseThrow(() -> new NotFoundException("Usuario bodeguero no encontrado con ID: " + bodegaDTO.getUsuarioId()));
                
                // Validar que tenga el rol adecuado (opcional pero recomendado)
                boolean esBodeguero = nuevoDueño.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equalsIgnoreCase("BODEGUERO") || r.getNombre().equalsIgnoreCase("ROLE_BODEGUERO"));
                
                if (!esBodeguero) {
                    throw new IllegalArgumentException("El usuario seleccionado no tiene permiso de Bodeguero");
                }

                // Asignar la relación
                bodega.setUsuario(nuevoDueño);
                // Asegurar bidireccionalidad (opcional dependiendo de cascada, pero seguro)
                nuevoDueño.getBodegas().add(bodega);
                
                LOGGER.info("🔄 Bodega #{} reasignada al usuario: {}", id, nuevoDueño.getEmail());
            }
        }

        bodegaRepository.save(bodega);
    }

    @Transactional
    public void delete(final Long id) {
        publisher.publishEvent(new BeforeDeleteBodega(id));
        bodegaRepository.deleteById(id);
    }

    // ======================================
    // CREAR BODEGA + ASIGNAR BODEGUERO
    // ======================================

    @Transactional
    public Long crearBodegaConBodeguero(final BodegaConBodegueroDTO dto) {
        LOGGER.info("🏪 Creando bodega con bodeguero...");
        Usuario bodeguero;

        if (Boolean.TRUE.equals(dto.getEsNuevoBodeguero())) {
            if (dto.getBodegueroNuevo() == null) {
                throw new IllegalArgumentException("Debe proporcionar los datos del bodeguero nuevo");
            }
            LOGGER.info("👤 Creando nuevo bodeguero: {}", dto.getBodegueroNuevo().getEmail());
            Long bodegueroId = usuarioService.crearBodeguero(dto.getBodegueroNuevo());
            bodeguero = usuarioService.getById(bodegueroId);  
        } else {
            if (dto.getBodegueroExistenteId() == null) {
                throw new IllegalArgumentException("Debe seleccionar un bodeguero existente");
            }
            LOGGER.info("👤 Asignando bodeguero existente ID: {}", dto.getBodegueroExistenteId());
            bodeguero = usuarioService.getById(dto.getBodegueroExistenteId());

            // Validación flexible de rol
            boolean esBodeguero = bodeguero.getRoles().stream()
                    .anyMatch(rol -> "BODEGUERO".equalsIgnoreCase(rol.getNombre()) || "ROLE_BODEGUERO".equalsIgnoreCase(rol.getNombre()));

            if (!esBodeguero) {
                throw new IllegalArgumentException("El usuario seleccionado no tiene rol BODEGUERO");
            }
        }

        LOGGER.info("🏪 Creando bodega: {}", dto.getBodega().getNombre());
        
        // Asignamos el usuario a la bodega ANTES de guardar si es posible
        Bodega bodegaEntity = new Bodega();
        mapToEntity(dto.getBodega(), bodegaEntity);
        bodegaEntity.setUsuario(bodeguero); // ✅ Asignación directa
        
        // Defaults
        if (bodegaEntity.getActivo() == null) bodegaEntity.setActivo(true);
        if (bodegaEntity.getLatitud() == null) bodegaEntity.setLatitud(-12.0464);
        if (bodegaEntity.getLongitud() == null) bodegaEntity.setLongitud(-77.0428);

        Bodega bodegaGuardada = bodegaRepository.save(bodegaEntity);
        
        // ✅ Inicializar métodos de pago por defecto
        inicializarMetodosPago(bodegaGuardada);
        
        // Actualizar lado inverso
        bodeguero.getBodegas().add(bodegaEntity);
        usuarioRepository.save(bodeguero);

        LOGGER.info("✅ Bodega creada y asociada correctamente");
        return bodegaEntity.getId();
    }

    // ======================================
    // DETALLE DE BODEGA
    // ======================================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleBodega(final Long id) {
        final Bodega bodega = bodegaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));

        final BodegaDTO dto = mapToDTO(bodega, new BodegaDTO());

        List<Map<String, Object>> bodegueros = bodega.getBodegueros().stream()
                .map(usuario -> Map.<String, Object>of(
                        "id", usuario.getId(),
                        "nombre", usuario.getNombre(),
                        "email", usuario.getEmail(),
                        "telefono", usuario.getTelefono() == null ? "" : usuario.getTelefono(),
                        "numeroDocumento", usuario.getNumeroDocumento() == null ? "" : usuario.getNumeroDocumento()
                ))
                .toList();

        Map<String, Object> estadisticas = Map.of(
                "totalProductos", bodega.getProductosBodega().size(),
                "totalPedidos", bodega.getPedidos().size()
        );

        return Map.of(
                "bodega", dto,
                "bodegueros", bodegueros,
                "estadisticas", estadisticas
        );
    }

    @Transactional
    public void asignarBodegueroAdicional(final Long bodegaId, final Long usuarioId) {
        Bodega bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new NotFoundException("Bodega no encontrada"));
        usuarioService.asociarConBodega(usuarioId, bodega);
        LOGGER.info("👤 Bodeguero adicional asignado a {}", bodega.getNombre());
    }

    // ======================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ======================================

    /**
     * Crea automáticamente los registros de YAPE y PLIN para una nueva bodega.
     */
    private void inicializarMetodosPago(Bodega bodega) {
        // Buscamos los tipos de pago globales
        List<String> tiposPorDefecto = List.of("YAPE", "PLIN", "EFECTIVO");

        for (String nombreTipo : tiposPorDefecto) {
            tipoMetodoPagoRepository.findByNombreIgnoreCase(nombreTipo).ifPresent(tipo -> {
                // Verificar si ya existe para no duplicar (en caso de re-runs)
                if (!bodegaMetodoPagoRepository.existsByBodegaIdAndTipoMetodoPagoId(bodega.getId(), tipo.getId())) {
                    BodegaMetodoPago metodo = new BodegaMetodoPago();
                    metodo.setBodega(bodega);
                    metodo.setTipoMetodoPago(tipo);
                    metodo.setActivo(true); // Activos por defecto, pero sin datos
                    metodo.setNombreTitular("");
                    metodo.setNumeroTelefono("");
                    bodegaMetodoPagoRepository.save(metodo);
                }
            });
        }
    }

    // ======================================
    // MAPEOS
    // ======================================

    private BodegaDTO mapToDTO(final Bodega bodega, final BodegaDTO dto) {
        dto.setId(bodega.getId());
        dto.setNombre(bodega.getNombre());
        dto.setDireccion(bodega.getDireccion());
        dto.setLatitud(bodega.getLatitud());
        dto.setLongitud(bodega.getLongitud());
        dto.setActivo(bodega.getActivo());
        dto.setTelefono(bodega.getTelefono());
        dto.setDistrito(bodega.getDistrito());
        dto.setHorario(bodega.getHorario());

        // ✅ AGREGADO: Enviar el ID del dueño actual al DTO
        if (bodega.getUsuario() != null) {
            dto.setUsuarioId(bodega.getUsuario().getId());
        }

        dto.setBodeguerosAsignados(
                usuarioRepository.countDistinctByBodegas_Id(bodega.getId())
        );

        return dto;
    }

    private Bodega mapToEntity(final BodegaDTO dto, final Bodega bodega) {
        bodega.setNombre(dto.getNombre());
        bodega.setDireccion(dto.getDireccion());
        bodega.setLatitud(dto.getLatitud());
        bodega.setLongitud(dto.getLongitud());
        bodega.setActivo(dto.getActivo());
        bodega.setTelefono(dto.getTelefono());
        bodega.setDistrito(dto.getDistrito());
        bodega.setHorario(dto.getHorario());
        return bodega;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getBodegaValues() {
        return bodegaRepository.findAll(Sort.by("id")).stream()
                .collect(CustomCollectors.toSortedMap(Bodega::getId, Bodega::getNombre));
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return bodegaRepository.count();
    }
}