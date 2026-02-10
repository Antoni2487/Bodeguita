package io.bootify.my_tiendita.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentoConsultaService {

    private final RestTemplate restTemplate;

    @Value("${miapi.token}")
    private String miapiToken;

    @Value("${miapi.url.dni}")
    private String dniUrl;

    @Value("${miapi.url.ruc}")
    private String rucUrl;

    public DocumentoConsultaService() {
        this.restTemplate = new RestTemplate();
    }

    // ===================== CONSULTA PRINCIPAL =====================
    public Map<String, Object> consultarDocumento(String documento) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            if (documento == null || documento.trim().isEmpty()) {
                return error("El documento es obligatorio");
            }

            documento = documento.trim();

            if (!documento.matches("^[0-9]{8}$|^[0-9]{11}$")) {
                return error("El documento debe ser DNI (8 dígitos) o RUC (11 dígitos)");
            }

            // === HEADERS ===
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + miapiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // =====================================
            //  DNI (8 dígitos)
            // =====================================
            if (documento.length() == 8) {

                String url = dniUrl + documento; // <-- CORRECTO

                ResponseEntity<DniResponseDTO> response =
                        restTemplate.exchange(url, HttpMethod.GET, entity, DniResponseDTO.class);

                DniResponseDTO body = response.getBody();

                if (body != null && body.isSuccess() && body.getDatos() != null) {

                    String nombreCompleto =
                            body.getDatos().getNombres() + " " +
                            body.getDatos().getApePaterno() + " " +
                            body.getDatos().getApeMaterno();

                    resultado.put("success", true);
                    resultado.put("tipo", "DNI");
                    resultado.put("nombre", nombreCompleto.trim());
                    resultado.put("documento", body.getDatos().getDni());

                    // Autollenar distrito si existe
                    if (body.getDatos().getDomiciliado() != null) {
                        resultado.put("distrito", body.getDatos().getDomiciliado().getDistrito());
                        resultado.put("direccion", body.getDatos().getDomiciliado().getDireccion());
                    }

                    return resultado;
                }

                return error("DNI no encontrado");
            }

            // =====================================
            //  RUC (11 dígitos)
            // =====================================
            if (documento.length() == 11) {

                String url = rucUrl + documento;

                ResponseEntity<RucResponseDTO> response =
                        restTemplate.exchange(url, HttpMethod.GET, entity, RucResponseDTO.class);

                RucResponseDTO body = response.getBody();

                if (body != null && body.isSuccess() && body.getDatos() != null) {

                    resultado.put("success", true);
                    resultado.put("tipo", "RUC");
                    resultado.put("nombre", body.getDatos().getRazonSocial());
                    resultado.put("documento", documento);

                    return resultado;
                }

                return error("RUC no encontrado");
            }

        } catch (Exception e) {
            return error("Error consultando API: " + e.getMessage());
        }

        return error("Formato inválido");
    }

    // ===================== ERROR =====================
    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", msg);
        return m;
    }

    // ===================== DTOs CORREGIDOS =====================

    @Getter @Setter
    public static class DniResponseDTO {
        private boolean success;

        @JsonProperty("datos")
        private DatosDni datos;

        @Getter @Setter
        public static class DatosDni {

            private String dni;
            private String nombres;

            @JsonProperty("ape_paterno")
            private String apePaterno;

            @JsonProperty("ape_materno")
            private String apeMaterno;

            private Domiciliado domiciliado;

            @Getter @Setter
            public static class Domiciliado {
                private String direccion;
                private String distrito;
                private String provincia;
                private String departamento;
                private String ubigeo;
            }
        }
    }

    @Getter @Setter
    public static class RucResponseDTO {
        private boolean success;

        @JsonProperty("datos")
        private DatosRuc datos;

        @Getter @Setter
        public static class DatosRuc {
            private String ruc;
            private String razonSocial;
            private String direccion;
            private String estado;
            private String condicion;
        }
    }
}
