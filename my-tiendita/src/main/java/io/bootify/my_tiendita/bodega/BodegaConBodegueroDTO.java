package io.bootify.my_tiendita.bodega;

import io.bootify.my_tiendita.usuario.UsuarioDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BodegaConBodegueroDTO {

    
    @NotNull(message = "Debe indicar si es bodeguero nuevo o existente")
    private Boolean esNuevoBodeguero;

    @Valid
    private UsuarioDTO bodegueroNuevo;

    private Long bodegueroExistenteId;

    @NotNull(message = "Los datos de la bodega son obligatorios")
    @Valid
    private BodegaDTO bodega;
}