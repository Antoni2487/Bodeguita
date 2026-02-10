package io.bootify.my_tiendita.categoria;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CategoriaDTO {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String nombre;

    @Size(max = 255)
    private String descripcion;

}