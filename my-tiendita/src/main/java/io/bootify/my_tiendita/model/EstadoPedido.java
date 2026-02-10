package io.bootify.my_tiendita.model;

public enum EstadoPedido {
    PENDIENTE,      // Cliente lo creó, pago pendiente o por confirmar
    CONFIRMADO,     // Bodega aceptó el pedido / Pago exitoso
    EN_PREPARACION, // Bodeguero armando paquete
    EN_CAMINO,      // Delivery en curso
    ENTREGADO,      // Cliente recibió
    CANCELADO       // Rechazado por bodega o cliente
}