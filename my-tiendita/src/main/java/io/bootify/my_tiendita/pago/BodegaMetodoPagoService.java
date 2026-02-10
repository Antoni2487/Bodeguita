package io.bootify.my_tiendita.pago;

import io.bootify.my_tiendita.util.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class BodegaMetodoPagoService {

    private final BodegaMetodoPagoRepository bodegaMetodoPagoRepository;

    // Inyectamos las mismas propiedades que usa tu FileUploadController
    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    public BodegaMetodoPagoService(BodegaMetodoPagoRepository bodegaMetodoPagoRepository) {
        this.bodegaMetodoPagoRepository = bodegaMetodoPagoRepository;
    }

    public List<BodegaMetodoPago> listarPorBodega(Long bodegaId) {
        return bodegaMetodoPagoRepository.findByBodegaId(bodegaId);
    }

    public BodegaMetodoPago obtenerPorId(Long id) {
        return bodegaMetodoPagoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Método de pago no encontrado: " + id));
    }

    @Transactional
    public void actualizarMetodoPago(Long id, String numeroTelefono, String nombreTitular, 
                                     Boolean activo, MultipartFile imagenQr) {
        
        BodegaMetodoPago metodo = obtenerPorId(id);

        metodo.setNumeroTelefono(numeroTelefono);
        metodo.setNombreTitular(nombreTitular);
        metodo.setActivo(activo != null ? activo : false);

        // Lógica de guardado de imagen (Reutilizando la lógica de tu FileUploadController)
        if (imagenQr != null && !imagenQr.isEmpty()) {
            try {
                String urlImagen = guardarImagen(imagenQr);
                metodo.setImagenQrUrl(urlImagen);
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar la imagen QR", e);
            }
        }

        bodegaMetodoPagoRepository.save(metodo);
    }

    /**
     * Método auxiliar para guardar la imagen en disco (Igual que en FileUploadController)
     */
    private String guardarImagen(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        String newFilename = UUID.randomUUID().toString() + "." + extension;
        String tipo = "pagos"; // Carpeta específica para QRs

        // Crear directorio
        Path uploadDir = Paths.get(uploadPath, tipo);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Guardar archivo
        Path filePath = uploadDir.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Retornar URL pública
        return urlPrefix + "/" + tipo + "/" + newFilename;
    }
}