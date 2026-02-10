package io.bootify.my_tiendita.solicitud_bodeguero;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudBodegueroDTO {

    private Long id;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Nombre del dueño o encargado")
    private String nombreSolicitante;

    @NotNull
    @Size(max = 150)
    @Email
    private String email;

    @NotNull
    @Size(max = 15)
    private String telefono;

    @NotNull
    @Size(max = 150)
    @Schema(description = "Nombre comercial de la bodega")
    private String nombreBodega;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Dirección física del local")
    private String direccionBodega;

    @Size(max = 20)
    @Schema(description = "RUC del negocio (opcional)")
    private String ruc;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String estado;
}