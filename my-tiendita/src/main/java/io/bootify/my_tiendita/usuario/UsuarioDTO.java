package io.bootify.my_tiendita.usuario;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UsuarioDTO {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String nombre;

    @NotNull
    @Size(max = 150)
    @UsuarioEmailUnique
    private String email;

    @Size(max = 255)
    private String password;

    @Size(max = 9)
    private String telefono;

    @Size(max = 100)
    private String direccion;

    private Double latitud;
    
    private Double longitud;

    private Boolean activo;

    private String rol;

    private String numeroDocumento;

    private Set<Long> bodegas = new HashSet<>();

}
