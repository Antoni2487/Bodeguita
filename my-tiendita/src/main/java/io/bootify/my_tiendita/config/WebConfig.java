package io.bootify.my_tiendita.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convierte "C:/uploads" en "file:C:/uploads/"
        String location = Paths.get(uploadPath).toUri().toString();

        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location);
    }
}