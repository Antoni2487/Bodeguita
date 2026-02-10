package io.bootify.my_tiendita.pedido;

import io.bootify.my_tiendita.pago.BodegaMetodoPagoDTO;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckoutResponseDTO {
    private boolean posible; 
    private String mensaje;  
    
    private BigDecimal subtotal;
    private BigDecimal costoDelivery;
    private BigDecimal total;
    private BigDecimal distanciaKm;
    private List<BodegaMetodoPagoDTO> metodosPago;
}