package io.bootify.my_tiendita.usuario;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.events.BeforeDeleteUsuario;
import io.bootify.my_tiendita.util.NotFoundException;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional  
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;
    private final RolRepository rolRepository;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            BodegaRepository bodegaRepository,
            RolRepository rolRepository,
            ApplicationEventPublisher publisher,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
        this.rolRepository = rolRepository;
        this.publisher = publisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> findAll() {
        return usuarioRepository.findAll(Sort.by("id")).stream()
                .map(usuario -> mapToDTO(usuario, new UsuarioDTO()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO get(final Long id) {
        return usuarioRepository.findById(id)
                .map(usuario -> mapToDTO(usuario, new UsuarioDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final UsuarioDTO usuarioDTO) {
        final Usuario usuario = new Usuario();
        mapToEntity(usuarioDTO, usuario);
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));

        // Asignar rol principal
        Rol rol = getRolOrThrow(usuarioDTO.getRol());
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);


        return usuarioRepository.save(usuario).getId();
    }

    public void update(final Long id, final UsuarioDTO usuarioDTO) {
        final Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        mapToEntity(usuarioDTO, usuario);

        if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        }

        // Actualizar rol principal
        if (usuarioDTO.getRol() != null) {
            usuario.getRoles().clear();
            Rol rol = rolRepository.findByNombre(usuarioDTO.getRol())
                    .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + usuarioDTO.getRol()));
            usuario.getRoles().add(rol);
        }

        usuarioRepository.save(usuario);
    }

    public void delete(final Long id) {
        final Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteUsuario(id));
        usuarioRepository.delete(usuario);
    }

    private UsuarioDTO mapToDTO(final Usuario usuario, final UsuarioDTO usuarioDTO) {
        usuarioDTO.setId(usuario.getId());
        usuarioDTO.setNombre(usuario.getNombre());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setTelefono(usuario.getTelefono());
        usuarioDTO.setDireccion(usuario.getDireccion());
        usuarioDTO.setActivo(usuario.getActivo());
        usuarioDTO.setLatitud(usuario.getLatitud());
        usuarioDTO.setLongitud(usuario.getLongitud());
        usuarioDTO.setNumeroDocumento(usuario.getNumeroDocumento());

        usuarioDTO.setRol(
            usuario.getRoles().stream()
                .findFirst()
                .map(Rol::getNombre)
                .orElse(null)
        );

        usuarioDTO.setBodegas(
                usuario.getBodegas().stream()
                        .map(Bodega::getId)
                        .collect(Collectors.toSet())
        );

        return usuarioDTO;
    }

    private Usuario mapToEntity(final UsuarioDTO usuarioDTO, final Usuario usuario) {
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setDireccion(usuarioDTO.getDireccion());
        usuario.setActivo(usuarioDTO.getActivo());
        usuario.setLatitud(usuarioDTO.getLatitud());
        usuario.setLongitud(usuarioDTO.getLongitud());
        usuario.setNumeroDocumento(usuarioDTO.getNumeroDocumento());

        if (usuarioDTO.getBodegas() != null) {
            usuario.setBodegas(
                    usuarioDTO.getBodegas().stream()
                            .map(id -> bodegaRepository.findById(id)
                                    .orElseThrow(() -> new NotFoundException("Bodega no encontrada: " + id)))
                            .collect(Collectors.toSet())
            );
        }

        return usuario;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerBodegueros() {
        // Buscamos usuarios que tengan CUALQUIERA de los dos formatos de rol
        List<Usuario> bodegueros = usuarioRepository.findAll(Sort.by("nombre")).stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equalsIgnoreCase("BODEGUERO") || 
                                       r.getNombre().equalsIgnoreCase("ROLE_BODEGUERO") ||
                                       r.getNombre().equalsIgnoreCase("ADMIN"))) // Opcional: Incluir admins si ellos también gestionan
                .toList();

        return bodegueros.stream()
                .map(usuario -> mapToDTO(usuario, new UsuarioDTO()))
                .toList();
    }

    public Long registrarCliente(final UsuarioDTO usuarioDTO) {
        final Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setDireccion(usuarioDTO.getDireccion());
        usuario.setActivo(true);

        Rol rolCliente = getRolOrThrow("CLIENTE");
        usuario.getRoles().add(rolCliente);

        return usuarioRepository.save(usuario).getId();
    }

    public Long crearBodeguero(final UsuarioDTO usuarioDTO) {
        final Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setDireccion(usuarioDTO.getDireccion());
        usuario.setActivo(true);
        usuario.setNumeroDocumento(usuarioDTO.getNumeroDocumento());

        Rol rolBodeguero = getRolOrThrow("BODEGUERO");
        usuario.getRoles().add(rolBodeguero);;

        return usuarioRepository.save(usuario).getId();
    }

    @Transactional // Importante para que guarde la relación en la tabla intermedia
    public void asociarConBodega(final Long usuarioId, final Bodega bodega) {
        final Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // ✅ CORRECCIÓN: Validamos "BODEGUERO" O "ROLE_BODEGUERO" (y también ADMIN por si acaso)
        boolean tienePermiso = usuario.getRoles().stream()
                .anyMatch(rol -> {
                    String nombreRol = rol.getNombre().toUpperCase();
                    return nombreRol.equals("BODEGUERO") || 
                           nombreRol.equals("ROLE_BODEGUERO") ||
                           nombreRol.equals("ADMIN") || 
                           nombreRol.equals("ROLE_ADMIN");
                });

        if (!tienePermiso) {
            throw new IllegalArgumentException("El usuario seleccionado no tiene rol de BODEGUERO ni ADMIN.");
        }

        // ✅ Evitar duplicados: Solo agregamos si no lo tiene ya
        if (!usuario.getBodegas().contains(bodega)) {
            usuario.getBodegas().add(bodega);
            usuarioRepository.save(usuario);
        }
    }
    @Transactional(readOnly = true)
    public Usuario getUsuarioEntity(final Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(final String email) {
        return usuarioRepository.existsByEmailIgnoreCase(email);
    }

    public void actualizarUbicacionPorEmail(final String email, final Double latitud, final Double longitud) {
        final Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        usuario.setLatitud(latitud);
        usuario.setLongitud(longitud);
        usuarioRepository.save(usuario);
    }


    // Metodos del dashboard

    @Transactional(readOnly = true)
    public long countAll() {
        return usuarioRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActivos() {
        return usuarioRepository.countByActivoTrue();
    }

    // Helper de buqueda Role_Admin o Admin

    private Rol getRolOrThrow(String rawName) {

    String plain = rawName.replaceFirst("^ROLE_", ""); // quita ROLE_ si viene
    return rolRepository.findByNombre(plain)
            .orElseThrow(() -> new NotFoundException("Rol no encontrado: " + plain));
    }
    // Metodo que recupera la entidad Usuario completa
    @Transactional(readOnly = true)
    public Usuario getById(final Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));
    }




}
