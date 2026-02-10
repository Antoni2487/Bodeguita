package io.bootify.my_tiendita.solicitud_bodeguero;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.notificacion.NotificacionService;
import io.bootify.my_tiendita.usuario.Rol;
import io.bootify.my_tiendita.usuario.RolRepository;
import io.bootify.my_tiendita.usuario.Usuario;
import io.bootify.my_tiendita.usuario.UsuarioRepository;
import io.bootify.my_tiendita.util.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolicitudBodegueroService {

    private final SolicitudBodegueroRepository solicitudBodegueroRepository;
    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;

    public SolicitudBodegueroService(final SolicitudBodegueroRepository solicitudBodegueroRepository,
                                     final UsuarioRepository usuarioRepository,
                                     final BodegaRepository bodegaRepository,
                                     final RolRepository rolRepository,
                                     final PasswordEncoder passwordEncoder,
                                     final NotificacionService notificacionService) {
        this.solicitudBodegueroRepository = solicitudBodegueroRepository;
        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacionService = notificacionService;
    }

    public List<SolicitudBodegueroDTO> findAll() {
        final List<SolicitudBodeguero> solicitudes = solicitudBodegueroRepository.findAll(Sort.by("id"));
        return solicitudes.stream()
                .map(solicitud -> mapToDTO(solicitud, new SolicitudBodegueroDTO()))
                .collect(Collectors.toList());
    }

    public SolicitudBodegueroDTO get(final Long id) {
        return solicitudBodegueroRepository.findById(id)
                .map(solicitud -> mapToDTO(solicitud, new SolicitudBodegueroDTO()))
                .orElseThrow(() -> new NotFoundException());
    }

    public Long create(final SolicitudBodegueroDTO solicitudBodegueroDTO) {
        // Validar email
        if (solicitudBodegueroRepository.existsByEmailIgnoreCase(solicitudBodegueroDTO.getEmail())) {
            throw new IllegalArgumentException("Ya existe una solicitud registrada con este correo.");
        }
        
        final SolicitudBodeguero solicitudBodeguero = new SolicitudBodeguero();
        mapToEntity(solicitudBodegueroDTO, solicitudBodeguero);
        solicitudBodeguero.setEstado("PENDIENTE");
        return solicitudBodegueroRepository.save(solicitudBodeguero).getId();
    }

    @Transactional
    public void aprobarSolicitud(Long id) {
        SolicitudBodeguero solicitud = solicitudBodegueroRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalArgumentException("La solicitud ya fue procesada");
        }

        // 1. Crear Usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(solicitud.getNombreSolicitante());
        nuevoUsuario.setEmail(solicitud.getEmail());
        nuevoUsuario.setTelefono(solicitud.getTelefono());
        nuevoUsuario.setDireccion(solicitud.getDireccionBodega()); // Usamos la dirección de la bodega por defecto
        nuevoUsuario.setActivo(true);
        // Contraseña temporal = El teléfono
        nuevoUsuario.setPassword(passwordEncoder.encode(solicitud.getTelefono()));
        
        // Asignar Rol BODEGUERO
        Rol rolBodeguero = rolRepository.findByNombre("BODEGUERO") // Asegúrate que tu BD tenga este rol o ajusta el nombre
                .orElseThrow(() -> new RuntimeException("Error: Rol BODEGUERO no encontrado en BD"));
        nuevoUsuario.setRoles(new HashSet<>(Collections.singletonList(rolBodeguero)));

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // 2. Crear Bodega
        Bodega nuevaBodega = new Bodega();
        nuevaBodega.setNombre(solicitud.getNombreBodega());
        nuevaBodega.setDireccion(solicitud.getDireccionBodega());
        nuevaBodega.setTelefono(solicitud.getTelefono());
        nuevaBodega.setActivo(true);
        nuevaBodega.setLatitud(0.0); // Valores por defecto (se actualizarán luego en perfil)
        nuevaBodega.setLongitud(0.0);
        
        // Vincular Usuario <-> Bodega
        nuevaBodega.setUsuario(usuarioGuardado); // Dueño principal
        
        // Guardar Bodega
        bodegaRepository.save(nuevaBodega);

        // 3. Actualizar Solicitud
        solicitud.setEstado("APROBADA");
        solicitudBodegueroRepository.save(solicitud);

        // 4. Notificar (Opcional: aquí podrías enviar un email real)
        System.out.println("✅ Solicitud Aprobada. Usuario creado: " + usuarioGuardado.getEmail());
    }

    @Transactional
    public void rechazarSolicitud(Long id) {
        SolicitudBodeguero solicitud = solicitudBodegueroRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
        
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalArgumentException("La solicitud ya fue procesada");
        }

        solicitud.setEstado("RECHAZADA");
        solicitudBodegueroRepository.save(solicitud);
    }

    public void delete(final Long id) {
        solicitudBodegueroRepository.deleteById(id);
    }

    // MAPPER METHODS
    private SolicitudBodegueroDTO mapToDTO(final SolicitudBodeguero solicitud, final SolicitudBodegueroDTO dto) {
        dto.setId(solicitud.getId());
        dto.setNombreSolicitante(solicitud.getNombreSolicitante());
        dto.setEmail(solicitud.getEmail());
        dto.setTelefono(solicitud.getTelefono());
        dto.setNombreBodega(solicitud.getNombreBodega());
        dto.setDireccionBodega(solicitud.getDireccionBodega());
        dto.setRuc(solicitud.getRuc());
        dto.setEstado(solicitud.getEstado());
        return dto;
    }

    private SolicitudBodeguero mapToEntity(final SolicitudBodegueroDTO dto, final SolicitudBodeguero solicitud) {
        solicitud.setNombreSolicitante(dto.getNombreSolicitante());
        solicitud.setEmail(dto.getEmail());
        solicitud.setTelefono(dto.getTelefono());
        solicitud.setNombreBodega(dto.getNombreBodega());
        solicitud.setDireccionBodega(dto.getDireccionBodega());
        solicitud.setRuc(dto.getRuc());
        return solicitud;
    }
}