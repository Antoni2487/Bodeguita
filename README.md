# Tu Bodeguita - Sistema de Gestión de Bodegas

Sistema web para digitalizar y modernizar bodegas tradicionales,
permitiéndoles competir frente a grandes cadenas como Mass y Tambo.
Proyecto académico desarrollado en la Universidad Tecnológica del Perú (UTP).

## Tecnologías

- Java Spring Boot (REST API, Spring Security, Spring Data JPA)
- Thymeleaf (Motor de plantillas)
- MySQL
- Bootstrap 5
- Google Maps API (Geolocalización y cálculo de distancias)
- HTML / CSS / JavaScript

## Funcionalidades

### Rol Administrador
- Dashboard con métricas del sistema (usuarios, bodegas, productos)
- Gestión de usuarios con tres roles: Admin, Bodeguero y Cliente
- Registro de bodegas con ubicación en mapa interactivo (Google Maps)
- Catálogo general de productos con categorías y subcategorías
- Consulta de DNI/RUC mediante API externa (miapi.cloud)
- Activación/desactivación de productos y usuarios

### Rol Bodeguero
- Dashboard con métricas propias: productos activos, stock bajo,
  pedidos pendientes y ventas del día
- Gestión de inventario con historial de movimientos (compras,
  mermas, devoluciones, ajustes)
- Registro de ventas manuales con ticket en tiempo real
- Gestión de pedidos online con estados (Pendiente → En Preparación
  → Listo → Completado)
- Configuración de delivery: precio por km, radio máximo,
  pedido mínimo y ubicación en mapa
- Métodos de pago: Efectivo, Yape, Plin

### Rol Cliente
- Catálogo público sin necesidad de registro
- Mapa de bodegas cercanas con Google Maps
- Carrito de compras con cálculo automático
- Pedidos con delivery o recojo en bodega
- Notificaciones de cambios de estado del pedido
- Historial de compras y perfil editable

## Estructuras de Datos Implementadas

- **Stack (Pila):** Gestión de notificaciones por usuario con
  principio LIFO (notificaciones más recientes primero)
- **Queue (Cola):** Cola de pedidos por bodega con principio FIFO
  (orden justo de atención)
- **ArrayList / LinkedList:** Manejo de detalles de ventas y
  validaciones de archivos

## Arquitectura

MVC (Modelo - Vista - Controlador) con arquitectura en capas:
- Controladores: @RestController (Spring Boot)
- Servicios: Lógica de negocio y reglas del sistema
- Repositorios: Spring Data JPA (acceso a datos)
- Modelos: Entidades JPA mapeadas a MySQL

## Base de Datos

18 tablas relacionadas: usuarios, bodegas, productos, inventario,
ventas, pedidos, notificaciones, configuración de delivery, y más.

## Cómo correrlo localmente

1. Clona el repositorio
   git clone https://github.com/Antoni2487/TuBodeguita.git

2. Crea la base de datos en MySQL
   CREATE DATABASE tubodeguita;

3. Importa el esquema
   mysql -u root -p tubodeguita < acceso.sql

4. Copia el archivo de configuración
   cp src/main/resources/application-example.properties
      src/main/resources/application.properties

5. Edita application.properties con tus datos:
   - URL de base de datos
   - Usuario y contraseña MySQL
   - Ruta local para uploads
   - Token de miapi.cloud

6. Ejecuta el proyecto desde IntelliJ o con Maven:
   ./mvnw spring-boot:run

7. Abre en el navegador: http://localhost:8082

## Proyecto Académico

Curso: Algoritmos y Estructura de Datos
Universidad Tecnológica del Perú · 2025
