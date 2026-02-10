package io.bootify.my_tiendita.estructuras;

import io.bootify.my_tiendita.pedido.Pedido;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Component
public class GestorColasPedidos {

    // Mapa: ID Bodega -> Cola de Pedidos
    private final Map<Long, Queue<Pedido>> colasPorBodega = new HashMap<>();

    public void encolar(Long bodegaId, Pedido pedido) {
        colasPorBodega.putIfAbsent(bodegaId, new LinkedList<>());
        colasPorBodega.get(bodegaId).offer(pedido); // offer = encolar al final
    }

    public Pedido desencolar(Long bodegaId) {
        if (!colasPorBodega.containsKey(bodegaId) || colasPorBodega.get(bodegaId).isEmpty()) {
            return null;
        }
        return colasPorBodega.get(bodegaId).poll(); // poll = sacar del frente
    }

    public Pedido verSiguiente(Long bodegaId) {
        if (!colasPorBodega.containsKey(bodegaId)) return null;
        return colasPorBodega.get(bodegaId).peek(); // peek = ver sin sacar
    }

    public Queue<Pedido> obtenerCola(Long bodegaId) {
        return colasPorBodega.getOrDefault(bodegaId, new LinkedList<>());
    }
}