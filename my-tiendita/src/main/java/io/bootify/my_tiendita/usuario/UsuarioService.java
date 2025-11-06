package io.bootify.my_tiendita.usuario;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import io.bootify.my_tiendita.events.BeforeDeleteBodega;
import io.bootify.my_tiendita.events.BeforeDeleteUsuario;
import io.bootify.my_tiendita.util.CustomCollectors;
import io.bootify.my_tiendita.util.NotFoundException;
import io.bootify.my_tiendita.util.ReferencedException;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;
    private final ApplicationEventPublisher publisher;

    public UsuarioService(final UsuarioRepository usuarioRepository,
            final BodegaRepository bodegaRepository, final ApplicationEventPublisher publisher) {
        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
        this.publisher = publisher;
    }

    public List<UsuarioDTO> findAll() {
        final List<Usuario> usuarios = usuarioRepository.findAll(Sort.by("id"));
        return usuarios.stream()
                .map(usuario -> mapToDTO(usuario, new UsuarioDTO()))
                .toList();
    }

    public UsuarioDTO get(final Long id) {
        return usuarioRepository.findById(id)
                .map(usuario -> mapToDTO(usuario, new UsuarioDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final UsuarioDTO usuarioDTO) {
        final Usuario usuario = new Usuario();
        mapToEntity(usuarioDTO, usuario);
        return usuarioRepository.save(usuario).getId();
    }

    public void update(final Long id, final UsuarioDTO usuarioDTO) {
        final Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(usuarioDTO, usuario);
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
        usuarioDTO.setPassword(usuario.getPassword());
        usuarioDTO.setTelefono(usuario.getTelefono());
        usuarioDTO.setDireccion(usuario.getDireccion());
        usuarioDTO.setActivo(usuario.getActivo());
        usuarioDTO.setBodegas(usuario.getBodegas() == null ? null : usuario.getBodegas().getId());
        return usuarioDTO;
    }

    private Usuario mapToEntity(final UsuarioDTO usuarioDTO, final Usuario usuario) {
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPassword(usuarioDTO.getPassword());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setDireccion(usuarioDTO.getDireccion());
        usuario.setActivo(usuarioDTO.getActivo());
        final Bodega bodegas = usuarioDTO.getBodegas() == null ? null : bodegaRepository.findById(usuarioDTO.getBodegas())
                .orElseThrow(() -> new NotFoundException("bodegas not found"));
        usuario.setBodegas(bodegas);
        return usuario;
    }

    public boolean emailExists(final String email) {
        return usuarioRepository.existsByEmailIgnoreCase(email);
    }

    public Map<Long, String> getUsuarioValues() {
        return usuarioRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Usuario::getId, Usuario::getNombre));
    }

    @EventListener(BeforeDeleteBodega.class)
    public void on(final BeforeDeleteBodega event) {
        final ReferencedException referencedException = new ReferencedException();
        final Usuario bodegasUsuario = usuarioRepository.findFirstByBodegasId(event.getId());
        if (bodegasUsuario != null) {
            referencedException.setKey("bodega.usuario.bodegas.referenced");
            referencedException.addParam(bodegasUsuario.getId());
            throw referencedException;
        }
    }

}
