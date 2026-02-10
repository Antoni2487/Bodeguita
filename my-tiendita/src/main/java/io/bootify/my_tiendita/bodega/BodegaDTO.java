package io.bootify.my_tiendita.bodega;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BodegaDTO {

    private Long id;

    @NotNull
    @Size(max = 150)
    private String nombre;

    @NotNull
    @Size(max = 255)
    private String direccion;

    @NotNull
    private Double latitud;

    @NotNull
    private Double longitud;

    @NotNull
    private Boolean activo;

    @Size(max = 15)
    private String telefono;

    @Size(max = 100)
    private String distrito;

    @Size(max = 100)
    private String horario;
    
    private Long bodeguerosAsignados;

    // ✅ NUEVO CAMPO: Para asignar/editar el dueño principal
    private Long usuarioId; 
}