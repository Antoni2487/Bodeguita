package io.bootify.my_tiendita.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoogleMapsService {

    @Value("${google.maps.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";

    // ============================================================
    //  AUTOCOMPLETE (Google Places API)
    // ============================================================
    public List<Map<String, Object>> autocompletar(String query) {
        try {
            // Coordenadas de Chiclayo para sesgo de resultados
            String location = "-6.7714,-79.8409";
            
            String url = UriComponentsBuilder.fromHttpUrl(PLACES_AUTOCOMPLETE_URL)
                    .queryParam("input", query)
                    .queryParam("key", apiKey)
                    .queryParam("language", "es")
                    .queryParam("components", "country:pe")
                    .queryParam("location", location)
                    .queryParam("radius", "50000") // 50km alrededor de Chiclayo
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !"OK".equals(body.get("status"))) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> predictions = (List<Map<String, Object>>) body.get("predictions");
            
            if (predictions == null) {
                return Collections.emptyList();
            }

            // Transformar a formato compatible con el frontend
            return predictions.stream()
                    .limit(5)
                    .map(pred -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("display_name", pred.get("description"));
                        item.put("place_id", pred.get("place_id"));
                        
                        // Extraer nombre corto si está disponible
                        Map<String, Object> structuredFormatting = 
                            (Map<String, Object>) pred.get("structured_formatting");
                        if (structuredFormatting != null) {
                            item.put("name", structuredFormatting.get("main_text"));
                        }
                        
                        return item;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error en autocompletar: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ============================================================
    // GEOCODIFICACIÓN (Dirección → Coordenadas)
    // ============================================================
    public Map<String, Object> geocodificar(String direccion) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Agregar contexto de Perú
            String fullAddress = direccion + ", Lambayeque, Peru";
            
            String url = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                    .queryParam("address", fullAddress)
                    .queryParam("key", apiKey)
                    .queryParam("language", "es")
                    .queryParam("region", "pe")
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !"OK".equals(body.get("status"))) {
                result.put("success", false);
                result.put("message", "No se encontró la dirección");
                return result;
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            
            if (results == null || results.isEmpty()) {
                result.put("success", false);
                result.put("message", "No se encontró la dirección");
                return result;
            }

            Map<String, Object> firstResult = results.get(0);
            Map<String, Object> geometry = (Map<String, Object>) firstResult.get("geometry");
            Map<String, Object> location = (Map<String, Object>) geometry.get("location");

            result.put("success", true);
            result.put("lat", location.get("lat"));
            result.put("lon", location.get("lng"));
            result.put("display_name", firstResult.get("formatted_address"));
            
            // Extraer distrito si está disponible
            List<Map<String, Object>> addressComponents = 
                (List<Map<String, Object>>) firstResult.get("address_components");
            
            if (addressComponents != null) {
                String distrito = extraerDistrito(addressComponents);
                if (distrito != null) {
                    result.put("distrito", distrito);
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error en geocodificar: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error al procesar la geocodificación");
            return result;
        }
    }

    // ============================================================
    // GEOCODIFICACIÓN INVERSA (Coordenadas → Dirección)
    // ============================================================
    public Map<String, Object> geocodificarInverso(double lat, double lon) {
    Map<String, Object> result = new HashMap<>();

    try {
        String latlng = lat + "," + lon;
        
        // ✅ OPCIÓN 1: Sin filtro (más flexible)
        String url = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                .queryParam("latlng", latlng)
                .queryParam("key", apiKey)
                .queryParam("language", "es")
                // ✅ Removemos result_type - Google lo dará en orden de relevancia
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null || !"OK".equals(body.get("status"))) {
            result.put("success", false);
            result.put("message", "No se pudo obtener la dirección");
            return result;
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
        
        if (results == null || results.isEmpty()) {
            result.put("success", false);
            result.put("message", "No se encontró dirección para estas coordenadas");
            return result;
        }

        // ✅ Tomar el primer resultado (el más relevante)
        Map<String, Object> firstResult = results.get(0);

        result.put("success", true);
        result.put("display_name", firstResult.get("formatted_address"));
        result.put("lat", lat);
        result.put("lon", lon);

        // Extraer distrito
        List<Map<String, Object>> addressComponents = 
            (List<Map<String, Object>>) firstResult.get("address_components");
        
        if (addressComponents != null) {
            String distrito = extraerDistrito(addressComponents);
            if (distrito != null) {
                result.put("distrito", distrito);
            }
        }

        return result;

        } catch (Exception e) {
            System.err.println("Error en geocodificarInverso: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error en geocodificación inversa: " + e.getMessage());
            return result;
        }
    }

    // ============================================================
    // HELPER: Extraer distrito de address_components
    // ============================================================
    private String extraerDistrito(List<Map<String, Object>> addressComponents) {
        // Buscar por tipo "locality" (ciudad/distrito)
        for (Map<String, Object> component : addressComponents) {
            List<String> types = (List<String>) component.get("types");
            
            if (types != null && types.contains("locality")) {
                return (String) component.get("long_name");
            }
        }
        
        // Fallback: buscar "administrative_area_level_3"
        for (Map<String, Object> component : addressComponents) {
            List<String> types = (List<String>) component.get("types");
            
            if (types != null && types.contains("administrative_area_level_3")) {
                return (String) component.get("long_name");
            }
        }
        
        return null;
    }
}