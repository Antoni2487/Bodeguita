package io.bootify.my_tiendita.subcategoria;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SubcategoriaDTO {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String nombre;

    private String descripcion;

    @NotNull
    private Long categoriaId;

}