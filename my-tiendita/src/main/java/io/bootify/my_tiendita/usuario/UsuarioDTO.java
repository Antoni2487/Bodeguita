package io.bootify.my_tiendita.usuario;

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

    @NotNull
    @Size(max = 255)
    private String password;

    @Size(max = 9)
    private String telefono;

    @Size(max = 100)
    private String direccion;

    @NotNull
    private Boolean activo;

    private Long bodegas;

}
