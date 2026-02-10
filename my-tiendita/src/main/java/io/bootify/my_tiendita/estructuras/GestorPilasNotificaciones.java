package io.bootify.my_tiendita.estructuras;

import io.bootify.my_tiendita.notificacion.Notificacion;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Component
public class GestorPilasNotificaciones {

    private final Map<Long, Stack<Notificacion>> pilasPorUsuario = new HashMap<>();

    public void apilar(Long usuarioId, Notificacion notificacion) {
        pilasPorUsuario.putIfAbsent(usuarioId, new Stack<>());
        pilasPorUsuario.get(usuarioId).push(notificacion); // push = poner arriba
    }

    public Notificacion desapilar(Long usuarioId) {
        if (!pilasPorUsuario.containsKey(usuarioId) || pilasPorUsuario.get(usuarioId).isEmpty()) {
            return null;
        }
        return pilasPorUsuario.get(usuarioId).pop(); // pop = sacar de arriba
    }

    public List<Notificacion> obtenerPilaComoLista(Long usuarioId) {
        if (!pilasPorUsuario.containsKey(usuarioId)) return new ArrayList<>();
        
        // Convertimos la pila a lista e invertimos para que el frontend vea el último primero
        List<Notificacion> lista = new ArrayList<>(pilasPorUsuario.get(usuarioId));
        Collections.reverse(lista); 
        return lista;
    }
}