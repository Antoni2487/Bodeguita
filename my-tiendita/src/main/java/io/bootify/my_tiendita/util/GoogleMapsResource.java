package io.bootify.my_tiendita.util;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google-maps")
public class GoogleMapsResource {

    private final GoogleMapsService service;

    public GoogleMapsResource(GoogleMapsService service) {
        this.service = service;
    }

    /**
     * AUTOCOMPLETE - Sugerencias de direcciones
     * GET /api/google-maps/autocomplete?q=Av Balta
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<Map<String, Object>>> autocomplete(
            @RequestParam("q") String query) {

        return ResponseEntity.ok(service.autocompletar(query));
    }

    /**
     * GEOCODIFICAR - Convertir dirección a coordenadas
     * GET /api/google-maps/geocode?direccion=Av Balta 510, Chiclayo
     */
    @GetMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocode(
            @RequestParam("direccion") String direccion) {

        return ResponseEntity.ok(service.geocodificar(direccion));
    }

    /**
     * GEOCODIFICACIÓN INVERSA - Convertir coordenadas a dirección
     * GET /api/google-maps/reverse?lat=-6.7714&lon=-79.8409
     */
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, Object>> reverse(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon) {

        return ResponseEntity.ok(service.geocodificarInverso(lat, lon));
    }
}