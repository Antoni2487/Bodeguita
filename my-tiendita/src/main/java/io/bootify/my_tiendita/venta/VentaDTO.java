package io.bootify.my_tiendita.venta;

import io.bootify.my_tiendita.detalle_venta.DetalleVentaDTO;
import io.bootify.my_tiendita.model.MetodoEntrega;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class VentaDTO {

    private Long id;

    private LocalDateTime fecha;

    private BigDecimal monto; 
    
    @NotNull
    private MetodoEntrega tipoEntrega;

    private BigDecimal costoDelivery;

    // ✅ COORDENADAS UNIFICADAS (usar solo estos)
    private Double latitudEntrega;
    private Double longitudEntrega;
    
    private String direccionEntrega;

    @NotNull
    private Long bodegaId;

    private Long bodegaMetodoPagoId;

    private String clienteNombre;

    private String estado;

    private String tipoMetodoPago;
    
    private String nombreMetodoPago;

    private List<DetalleVentaDTO> productos; // Para recibir del carrito
    
    private List<DetalleVentaDTO> detalles; // Para enviar al modal
    
    // ✅ Getter auxiliar para compatibilidad con frontend
    public Double getLatitudCliente() {
        return latitudEntrega;
    }
    
    public void setLatitudCliente(Double latitud) {
        this.latitudEntrega = latitud;
    }
    
    public Double getLongitudCliente() {
        return longitudEntrega;
    }
    
    public void setLongitudCliente(Double longitud) {
        this.longitudEntrega = longitud;
    }
    
    // ✅ Getter auxiliar para montoTotal
    public BigDecimal getMontoTotal() {
        return monto;
    }
    
    public void setMontoTotal(BigDecimal montoTotal) {
        this.monto = montoTotal;
    }
    
    // ✅ Getter auxiliar para nombreCliente
    public String getNombreCliente() {
        return clienteNombre;
    }
    
    public void setNombreCliente(String nombreCliente) {
        this.clienteNombre = nombreCliente;
    }
}