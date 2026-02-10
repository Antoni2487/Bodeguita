package io.bootify.my_tiendita.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 🔧 CSRF: Deshabilitado para API REST
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/register", "/login")
            )
            
            // 🔐 Autorización de rutas
            .authorizeHttpRequests(auth -> auth
                // ========================================
                // 1. RECURSOS ESTÁTICOS (CSS, JS, IMAGES)
                // ========================================
                
                .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/webjars/**", "/favicon.ico", "/uploads/**").permitAll()

                .requestMatchers("/uploads/**").permitAll()

                // ========================================
                // 2. MÓDULO CLIENTE - VISTAS PÚBLICAS
                // ========================================
                .requestMatchers("/", "/home", "/index").permitAll()                    // Landing Page
                .requestMatchers("/cliente", "/cliente/").permitAll()                   // Inicio cliente
                .requestMatchers("/cliente/explorar").permitAll()                       // Mapa de bodegas
                .requestMatchers("/cliente/bodega/**").permitAll()                      // Catálogo por bodega
                .requestMatchers("/cliente/carrito").permitAll()                        // Carrito (lectura)
                .requestMatchers("/cliente/trabaja-con-nosotros").permitAll()           // Formulario afiliación
                .requestMatchers("/trabaja-con-nosotros").permitAll()                   // Alias del formulario
                
                // ========================================
                // 3. MÓDULO CLIENTE - VISTAS PROTEGIDAS
                // ========================================
                .requestMatchers("/cliente/mis-pedidos").authenticated()                 // Historial de pedidos
                .requestMatchers("/cliente/perfil").authenticated()                      // Perfil del cliente
                
                // ========================================
                // 4. API REST PÚBLICA (Módulo Cliente)
                // ========================================
                .requestMatchers("/api/cliente/bodegas/**").permitAll()                  // Lista de bodegas
                .requestMatchers("/api/cliente/productos/**").permitAll()                // Catálogo de productos
                .requestMatchers("/api/cliente/inicio").permitAll()                      // Datos del inicio
                
                // ========================================
                // 5. API REST PROTEGIDA (Requiere login)
                // ========================================
                .requestMatchers("/api/cliente/pedidos/pre-checkout").permitAll()
                .requestMatchers("/api/cliente/pedidos/**").authenticated()              // Crear pedidos
                .requestMatchers("/api/cliente/solicitudes").permitAll()                 // Solicitud bodeguero (público)

                // ========================================
                // 6. OTRAS APIs PÚBLICAS (Ya existentes)
                // ========================================
                .requestMatchers("/api/subcategorias/form-data/**").permitAll()
                .requestMatchers("/api/productos/form-data/**").permitAll()
                .requestMatchers("/api/bodegas/form-data/**").permitAll()
                .requestMatchers("/api/google-maps/**").permitAll()
                .requestMatchers("/api/consultar-documento/**").permitAll()
                
                // ========================================
                // 7. MÓDULO TIENDA (Si existe, mantener)
                // ========================================
                .requestMatchers("/tienda/**").permitAll()                               // Catálogo y Carrito (Lectura)
                
                // ========================================
                // 8. API REST PROTEGIDA (General)
                // ========================================
                .requestMatchers("/api/**").authenticated()                              // Resto de APIs requieren login
                
                // ========================================
                // 9. VISTAS PROTEGIDAS POR ROL
                // ========================================
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/bodeguero/**").hasRole("BODEGUERO")

                // ========================================
                // 10. AUTENTICACIÓN
                // ========================================
                .requestMatchers("/login", "/register").permitAll()
                
                // Resto requiere autenticación
                .anyRequest().authenticated()
            )
            
            // 🔑 Login personalizado
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    String role = authentication.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                        .orElse("CLIENTE"); 

                    switch (role) {
                        case "ADMIN" -> response.sendRedirect("/admin/dashboard");
                        case "BODEGUERO" -> response.sendRedirect("/bodeguero/dashboard");
                        case "CLIENTE" -> response.sendRedirect("/cliente");  // ← Redirige a /cliente
                        default -> response.sendRedirect("/cliente");
                    }
                })
                .permitAll()
            )
            
            // 🚪 Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}