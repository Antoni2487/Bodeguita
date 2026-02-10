package io.bootify.my_tiendita.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración para servir archivos estáticos desde una carpeta externa
 * 
 * 
 * Las imágenes serán accesibles mediante: http://localhost:8080/uploads/productos/imagen.jpg
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convertir la ruta a URI compatible con file://
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadLocation = uploadDir.toUri().toString();

        // Registrar el handler para servir archivos estáticos
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(uploadLocation);

        // Log para debugging
        System.out.println("========================================");
        System.out.println(" Configuración de uploads:");
        System.out.println("   Carpeta física: " + uploadDir);
        System.out.println("   URL de acceso: " + urlPrefix + "/**");
        System.out.println("   Ejemplo: http://localhost:8086" + urlPrefix + "/productos/imagen.jpg");
        System.out.println("========================================");
    }
}