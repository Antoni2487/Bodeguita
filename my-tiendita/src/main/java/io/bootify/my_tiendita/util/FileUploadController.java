package io.bootify.my_tiendita.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_TYPES = Arrays.asList("productos", "marcas", "categorias", "personalizacion");
 
    /**
     * Endpoint para subir UNA imagen
     */
    @PostMapping("/{tipo}/imagen")
    public ResponseEntity<Map<String, Object>> uploadImagen(
            @PathVariable String tipo,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validar tipo permitido
            if (!ALLOWED_TYPES.contains(tipo.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo no válido. Permitidos: " + ALLOWED_TYPES);
                return ResponseEntity.badRequest().body(response);
            }

            // Validar archivo
            String validationError = validateFile(file);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename).toLowerCase();
            String newFilename = UUID.randomUUID().toString() + "." + extension;

            // Crear directorio específico para el tipo (uploads/productos, uploads/marcas, etc.)
            Path uploadDir = Paths.get(uploadPath, tipo);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Directorio creado: {}", uploadDir);
            }

            // Guardar archivo
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL pública del archivo
            String fileUrl = urlPrefix + "/" + tipo + "/" + newFilename;

            response.put("success", true);
            response.put("message", "Imagen subida exitosamente");
            response.put("url", fileUrl);
            response.put("filename", newFilename);
            response.put("originalFilename", originalFilename);
            response.put("size", file.getSize());
            response.put("tipo", tipo);

            log.info("Imagen subida: {} -> {}", originalFilename, fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error al guardar archivo", e);
            response.put("success", false);
            response.put("message", "Error al guardar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Error inesperado", e);
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para eliminar una imagen

     */
    @DeleteMapping("/{tipo}/imagen/{filename}")
    public ResponseEntity<Map<String, Object>> deleteImagen(
            @PathVariable String tipo,
            @PathVariable String filename) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validar tipo permitido
            if (!ALLOWED_TYPES.contains(tipo.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo no válido");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar filename (prevenir path traversal)
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                response.put("success", false);
                response.put("message", "Nombre de archivo inválido");
                return ResponseEntity.badRequest().body(response);
            }

            Path filePath = Paths.get(uploadPath, tipo, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                response.put("success", true);
                response.put("message", "Imagen eliminada exitosamente");
                log.info("Imagen eliminada: {}", filePath);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Archivo no encontrado");
                log.warn("Archivo no encontrado: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IOException e) {
            log.error("Error al eliminar archivo", e);
            response.put("success", false);
            response.put("message", "Error al eliminar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Error inesperado al eliminar", e);
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Valida un archivo de imagen
     */
    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "El archivo está vacío";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "El archivo excede el tamaño máximo permitido (5MB)";
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return "Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG, GIF y WEBP";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "Nombre de archivo inválido";
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "Extensión de archivo no permitida";
        }

        return null;
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
