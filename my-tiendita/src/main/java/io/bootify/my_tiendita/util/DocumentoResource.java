package io.bootify.my_tiendita.util;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/consultar-documento")
public class DocumentoResource {

    private final DocumentoConsultaService documentoConsultaService;

    public DocumentoResource(final DocumentoConsultaService documentoConsultaService) {
        this.documentoConsultaService = documentoConsultaService;
    }

    @GetMapping("/{numero}")
    @ApiResponse(responseCode = "200")
    public ResponseEntity<Map<String, Object>> consultarDocumento(
            @PathVariable(name = "numero") final String numero) {
        
        Map<String, Object> resultado = documentoConsultaService.consultarDocumento(numero);
        
        // Si la consulta fue exitosa, devolver 200 OK
        // Si hubo error, también 200 pero con success: false en el body
        return ResponseEntity.ok(resultado);
    }
}