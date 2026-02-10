package io.bootify.my_tiendita.pago;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BodegaMetodoPagoDTO {


    private Long id;
    private Long tipoMetodoPagoId;
    private String tipoMetodoPagoNombre;
    private String nombreTitular;
    private String numeroTelefono;
    private String imagenQrUrl;
    private Boolean activo;
    private Long bodega;
}