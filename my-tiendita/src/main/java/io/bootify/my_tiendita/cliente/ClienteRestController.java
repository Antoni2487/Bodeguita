package io.bootify.my_tiendita.cliente;

import io.bootify.my_tiendita.bodega.Bodega;
import io.bootify.my_tiendita.bodega.BodegaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API para el módulo CLIENTE
 * Proporciona endpoints JSON para consumo desde JavaScript
 */
@RestController
@RequestMapping("/api/cliente")
public class ClienteRestController {

    private final BodegaRepository bodegaRepository;

    public ClienteRestController(final BodegaRepository bodegaRepository) {
        this.bodegaRepository = bodegaRepository;
    }

    // ========================================
    // API: BODEGAS CERCANAS (Para el Mapa)
    // ========================================
    
    /**
     * Endpoint para obtener bodegas cercanas con distancia calculada
     * Usado por el mapa en explorar.html
     * 
     * Ruta: GET /api/cliente/bodegas/cercanas?lat=-6.7714&lng=-79.8411&radio=5000
     * 
     * @param lat Latitud del usuario
     * @param lng Longitud del usuario
     * @param radio Radio de búsqueda en metros (default: 5000m = 5km)
     * @return Lista de bodegas con distancia calculada y ordenadas por cercanía
     */
    @GetMapping("/bodegas/cercanas")
    public List<BodegaCercanaDTO> obtenerBodegasCercanas(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5000") Integer radio) {
        
        // 1. Obtener todas las bodegas activas
        List<Bodega> bodegas = bodegaRepository.findAllByActivoTrue();
        
        // 2. Calcular distancia y filtrar por radio
        return bodegas.stream()
                .map(bodega -> {
                    double distanciaKm = calcularDistancia(
                            lat, lng, 
                            bodega.getLatitud(), 
                            bodega.getLongitud()
                    );
                    
                    return new BodegaCercanaDTO(
                            bodega.getId(),
                            bodega.getNombre(),
                            bodega.getDireccion(),
                            bodega.getDistrito(),
                            bodega.getTelefono(),
                            bodega.getHorario(),
                            bodega.getLatitud(),
                            bodega.getLongitud(),
                            distanciaKm,
                            formatearDistancia(distanciaKm)
                    );
                })
                .filter(dto -> dto.getDistanciaKm() <= (radio / 1000.0)) // Filtrar por radio
                .sorted((a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm())) // Ordenar por distancia
                .collect(Collectors.toList());
    }

    // ========================================
    // CÁLCULO DE DISTANCIA (Fórmula de Haversine)
    // ========================================
    
    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
     * 
     * @param lat1 Latitud punto 1
     * @param lon1 Longitud punto 1
     * @param lat2 Latitud punto 2
     * @param lon2 Longitud punto 2
     * @return Distancia en kilómetros
     */
    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int RADIO_TIERRA_KM = 6371; // Radio de la Tierra en kilómetros
        
        // Convertir grados a radianes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        // Fórmula de Haversine
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return RADIO_TIERRA_KM * c;
    }

    /**
     * Formatea la distancia para mostrar en UI de forma amigable
     * 
     * @param distanciaKm Distancia en kilómetros
     * @return String formateado ("500m" o "2.3km")
     */
    private String formatearDistancia(double distanciaKm) {
        if (distanciaKm < 1) {
            // Menos de 1 km, mostrar en metros
            return String.format("%.0fm", distanciaKm * 1000);
        } else {
            // 1 km o más, mostrar en kilómetros con 1 decimal
            return String.format("%.1fkm", distanciaKm);
        }
    }

    // ========================================
    // DTO: BODEGA CERCANA
    // ========================================
    
    /**
     * DTO para la respuesta de bodegas cercanas
     * Incluye información básica de la bodega + distancia calculada
     */
    public static class BodegaCercanaDTO {
        private Long id;
        private String nombre;
        private String direccion;
        private String distrito;
        private String telefono;
        private String horario;
        private Double latitud;
        private Double longitud;
        private Double distanciaKm;
        private String distanciaFormateada;

        public BodegaCercanaDTO(Long id, String nombre, String direccion, String distrito,
                                String telefono, String horario, Double latitud, Double longitud,
                                Double distanciaKm, String distanciaFormateada) {
            this.id = id;
            this.nombre = nombre;
            this.direccion = direccion;
            this.distrito = distrito;
            this.telefono = telefono;
            this.horario = horario;
            this.latitud = latitud;
            this.longitud = longitud;
            this.distanciaKm = distanciaKm;
            this.distanciaFormateada = distanciaFormateada;
        }

        // Getters (necesarios para la serialización JSON)
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        public String getDireccion() { return direccion; }
        public String getDistrito() { return distrito; }
        public String getTelefono() { return telefono; }
        public String getHorario() { return horario; }
        public Double getLatitud() { return latitud; }
        public Double getLongitud() { return longitud; }
        public Double getDistanciaKm() { return distanciaKm; }
        public String getDistanciaFormateada() { return distanciaFormateada; }
    }
}