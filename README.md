# 🛡️ Tokenización App

**Versión:** 1.1  
**Fecha:** 25-ago-2025  
**Runtime:** Java 21 · Spring Boot 3  
**Perfiles activos:** `dev` / `prod`  

---

## 📌 Descripción

**Tokenización App** es una **API REST** desarrollada en **Spring Boot** que gestiona operaciones de **tokenización** (tarjetas, entidades, identificadores u otros dominios análogos).  

Incluye:
- Arquitectura **hexagonal ligera** (puertos/adaptadores).  
- Persistencia en **PostgreSQL** (con soporte en `dev` para **H2**).  
- **Contenedorización con Docker** + `docker-compose`.  
- **Colección Postman** para pruebas E2E y de errores.  
- Despliegue en plataformas **Render** (prod), Railway, Fly.io y GCP Cloud Run.  

👉 **API ya desplegada en Render:**  
🔗 [https://tokenizacion.onrender.com](https://tokenizacion.onrender.com)

---

## ⚙️ Tecnologías principales

- Java 21  
- Spring Boot 3 (Web, Data JPA, Validation, Actuator)  
- PostgreSQL 16 / H2  
- Docker & Docker Compose  
- OpenAPI/Swagger (opcional)  
- JUnit 5 + Mockito + Testcontainers  

---

## 🏗️ Arquitectura

- **API (Controller):** Endpoints REST, validación, manejo de errores.  
- **Servicio (Application):** Casos de uso y reglas de negocio.  
- **Dominio:** Entidades + interfaces (puertos).  
- **Infraestructura:** Adaptadores (JPA, configuración, clientes externos).  

---

## 🔑 Configuración de Entornos

Variables recomendadas:

SPRING_PROFILES_ACTIVE=dev         # o prod
SERVER_PORT=8080

# Base de datos
SPRING_DATASOURCE_URL=jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME
SPRING_DATASOURCE_USERNAME=$DB_USER
SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD

La aplicación está pensada para ser flexible en su despliegue y pruebas. Existen varias formas de correrla:

- **Modo dev (local con H2 en memoria):**  
  Descargando el repositorio y ejecutando la aplicación con Maven, se utiliza la base de datos en memoria H2, ideal para desarrollo rápido y pruebas locales.  
  Este modo también soporta opcionalmente MailHog para visualizar los correos enviados.

- **Modo prod (local con PostgreSQL real):**  
  Configurando la aplicación con el perfil `prod`, la persistencia se realiza sobre PostgreSQL y se aplican migraciones con Flyway.  
  Este es el modo pensado para entornos de producción.

- **Imagen Docker (por defecto en prod):**  
  El repositorio incluye un `Dockerfile` multistage que genera una imagen lista para ejecutar la aplicación en modo producción.  
  Con este enfoque no es necesario instalar dependencias localmente; basta con construir y correr el contenedor.

- **Servicio ya desplegado en Render:**  
  Para mayor comodidad, la aplicación ya se encuentra desplegada en la nube en la siguiente URL:  
  👉 https://tokenizacion.onrender.com  

  Esto permite que no sea necesario levantar nada en local si solo se quieren probar los endpoints.  
  Los archivos `.json` incluidos en el repositorio contienen la colección de **Postman**, con los requests y tests automatizados.  
  Para usarlos contra la API en Render, solo debes cambiar la variable `baseUrl` a la dirección anterior, y podrás probar inmediatamente todas las operaciones (ping, productos, carrito, tokenización de tarjetas y órdenes).  
  Ten en cuenta que el entorno desplegado en Render corre bajo el perfil de producción, por lo que está utilizando PostgreSQL y configuración real de persistencia.

---

1) Instrucciones para ejecutar localmente

Requisitos:
- Java 21, Maven 3.9+
- Docker (solo si quieres MailHog para ver correos)
- Postman/Insomnia para pruebas manuales

Variables de entorno mínimas:
# Seguridad
export API_KEY=dev-123

# Clave AES-GCM (recomendado 32 bytes en base64)
# macOS/Linux:
export AES_GCM_KEY_BASE64=$(openssl rand -base64 32)

# Email (dev con MailHog; desactiva si no lo usas)
export MAIL_ENABLED=true
export MAIL_FROM=noreply@local.test
export MAIL_HOST=localhost
export MAIL_PORT=1025
export MAIL_USERNAME=
export MAIL_PASSWORD=

> En producción usarás PostgreSQL/SMTP reales y no dejarás defaults.

Opción A: Ejecutar con Maven (H2 en memoria)

(Opcional) levanta MailHog para ver emails:

docker run --rm -p 1025:1025 -p 8025:8025 mailhog/mailhog
# UI: http://localhost:8025

Arranca la app:

./mvnw spring-boot:run

Prueba rápido:

curl http://localhost:8080/api/ping
# pong

> Para endpoints /api/** debes enviar X-API-KEY: dev-123.

Opción B (opcional): Docker (sin preparar nada local)

docker build -t tokenizacion-app:latest .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e API_KEY=dev-123 \
  -e AES_GCM_KEY_BASE64="$(openssl rand -base64 32)" \
  -e MAIL_ENABLED=false \
  tokenizacion-app:latest

2) Descripción del sistema y sus componentes

Visión general:
- REST API con Spring Boot 3: Web, Validation, Security, JPA/Hibernate.
- Persistencia: H2 (dev) y PostgreSQL (prod). Migraciones con Flyway.
- Seguridad: header X-API-KEY mediante filtro (rechaza 401 si falta o es inválida).
- Criptografía: AesGcmStringCryptoConverter (AES-GCM) para datos sensibles en DB.
  Clave: AES_GCM_KEY_BASE64 (16/24/32 bytes base64 — recomendado 32).

Dominio:
- User
- Product (precio BigDecimal, @Version para bloqueo optimista)
- Cart / CartItem
- Card (tokenizada, maskedNumber, cvv cifrado)
- Order / OrderItem (snapshot de unitPrice, total)

Pagos simulados:
- Reintentos con probabilidad de rechazo configurable:
  - payment.rejection-probability (default 0.3)
  - payment.max-attempts (default 3)

Emails:
- MailService + listener con @TransactionalEventListener(AFTER_COMMIT) y @Async:
  - Aprobada → correo de confirmación
  - Rechazada tras 3 intentos → correo de rechazo

Errores:
- GlobalExceptionHandler → ProblemDetail (RFC-7807) con timestamp, path, y lista errors para validaciones.

Observabilidad:
- Spring Actuator (health, info, metrics)

Paquetes (orientativo):
- controller/ – Exposición REST (CartController, OrderController, ProductController…)
- service/ – Reglas de negocio (OrderService, CartService, MailService…)
- repository/ – Repositorios JPA
- model/entity/ – Entidades JPA
- dto/ – DTOs de entrada/salida con Bean Validation
- security/ – Filtro y config de API Key
- web/ – GlobalExceptionHandler
- events/ listeners/ – Eventos de orden + envío de email
- config/ – Config async, mail, etc.

Perfiles y configuración:
- dev (por defecto): H2 en memoria, consola H2 habilitada, logging detallado, MailHog opcional.
- prod: PostgreSQL, Flyway obligatorio (ddl-auto: validate), sin mostrar SQL en logs.

Variables relevantes (también desde application.yml):
- security.api-key
- crypto.aes-gcm.key-base64
- payment.rejection-probability, payment.max-attempts
- business.min-stock-visible
- spring.mail.* (host, puerto, credenciales)

3) Endpoints principales (con ejemplos)

Recuerda incluir X-API-KEY: dev-123 en los protegidos (/api/**).

Salud:
GET /api/ping → 200 "pong"

Productos:
GET /api/products → 200 [ { "id":1, "name":"Teclado", "price":180000, "stock":10 }, ... ]

Carrito:
POST /api/cart/add?userId=1&productId=10&quantity=2 → 200 CartDTO
GET /api/cart/1 → 200 CartDTO (items con productId, productName, quantity)

Tarjetas (tokenización):
POST /api/cards/tokenize
Content-Type: application/json
{
  "number": "4111111111111111",
  "cvv": "123",
  "expiryDate": "2028-12",
  "userId": 1
}
→ 200/201 { token, maskedNumber, ... }

Órdenes:

A) Desde carrito:
POST /api/orders?userId=1&address=Calle Falsa 123
Content-Type: application/json
{ "cardToken": "..." }
→ 200 { "id": 5, "status": "APPROVED"|"REJECTED", "attempts": n }

B) Con productos explícitos:
POST /api/orders?userId=1&address=Calle Falsa 123
Content-Type: application/json
{
  "cardToken": "...",
  "products": { "10": 1, "12": 2 }
}
→ 200 { "id":..., "status":..., "attempts":... }

> En aprobada: se descuenta stock, se limpia carrito y se envía email.
> En rechazada: tras 3 intentos se envía email de rechazo.
> Control de concurrencia: @Version en Product (conflictos devuelven 409 via handler).

4) Cómo correr las pruebas

4.1 Unitarias / de integración (Maven)
# todas las pruebas
./mvnw test

# build completo
./mvnw verify

Sugerencias de escenarios:

Forzar aprobado:
PAYMENT_REJECTION_PROB=0 ./mvnw test

Forzar rechazo:
PAYMENT_REJECTION_PROB=1 ./mvnw test

Concurrencia / stock bajo: dos órdenes simultáneas sobre el mismo producto → esperar un 409 (optimistic lock).

Si tienes pruebas que requieren correo pero no hay SMTP, pon MAIL_ENABLED=false para evitar warnings.

4.2 Colección Postman (manual y automatizada)

Importa el archivo de colección (.postman_collection.json) en Postman.

Configura un Environment con variables:
- baseUrl = http://localhost:8080   (o https://tokenizacion.onrender.com para el servicio ya desplegado)
- apiKey = dev-123
- userId = 1 (o el que uses)

Orden recomendado de ejecución:
1. Ping
2. Productos - Listar (guarda productId)
3. Carrito - Agregar y Carrito - Ver
4. Tarjetas - Tokenizar (guarda cardToken)
5. Orden - Desde carrito o Orden - con products
6. Orden - Obtener por id
7. Logs

La colección incluye tests negativos:
- Sin/Inválida API Key → 401
- Validaciones (stock/quantity/cvv) → 400/422
- Producto inexistente → 4xx

Para ver correos en dev, abre MailHog: http://localhost:8025

5) Troubleshooting (rápido)

AES_GCM_KEY_BASE64 no configurado
→ Exporta una clave base64 válida (16/24/32 bytes). Recomendado 32:
export AES_GCM_KEY_BASE64=$(openssl rand -base64 32)

401 Unauthorized en /api/**
→ Falta header X-API-KEY o es incorrecto (API_KEY env var).

failed to lazily initialize ... no Session
→ Usa endpoints/repos con fetch join o DTOs (ya incluido en servicios).

Email “Connection refused”
→ Levanta MailHog (MAIL_HOST=localhost, MAIL_PORT=1025) o desactiva con MAIL_ENABLED=false.

Conflictos de stock bajo/concurrencia
→ Espera 409 por bloqueo optimista; vuelve a intentar o maneja en cliente.

6) IA
________________________________________
Durante el desarrollo del proyecto Tokenización App, se utilizó el modelo de lenguaje GPT-5 de OpenAI como asistente de ingeniería de software. El objetivo principal fue agilizar la construcción, documentación y despliegue de la API, garantizando buenas prácticas de desarrollo en Spring Boot, contenedorización con Docker y despliegue en plataformas en la nube como Rende.

Promt inicial: Te voy a pasar un documento con un reto tecnico y adicionalmente te voy a pasar el progreso que llevo, necesito que revises si todo esta correcto y estoy cumpliendo con los requerimientos que se piden
  Dicho chat se encuentra en esta URL: https://chatgpt.com/share/68ab4527-5a68-8001-bc2d-c49a3103e0f6
________________________________________
2. Áreas de apoyo de GPT-5
2.1 Diseño de arquitectura
•	Se consultaron patrones de arquitectura para aplicaciones REST, optando por una arquitectura hexagonal ligera.
•	Recomendación de capas: Controller – Service – Domain – Infraestructura.
•	Definición de estándares de validación, manejo de errores y DTOs.
2.2 Configuración del proyecto
•	Generación de ejemplos de configuración para application.properties y uso de variables de entorno.
•	Buenas prácticas en Spring Profiles (dev / prod).
•	Asesoría para el manejo de HikariCP y optimización de pool de conexiones.
2.3 Persistencia y base de datos
•	Generación de entidades con anotaciones JPA/Hibernate.
•	Uso de identificadores UUID/Long según requerimientos.
•	Configuración de PostgreSQL y fallback a H2 en desarrollo.
•	Explicación de errores comunes: Invalid port number ${DB_PORT}, Connection refused.
2.4 Contenedorización y despliegue
•	Creación de un Dockerfile optimizado para Java 21 con Spring Boot.
•	Configuración de docker-compose.yml (API + PostgreSQL con healthcheck).
•	Instrucciones para despliegue en Render, Railway, Fly.io y GCP Cloud Run.
•	Recomendación de usar DBaaS gestionadas en lugar de contenedores de DB en producción.
2.5 Pruebas y aseguramiento de calidad
•	Generación de colecciones Postman para pruebas funcionales y de error.
•	Instrucciones para ejecutar pruebas automáticas con Newman.
•	Sugerencias para pruebas unitarias e integración con JUnit 5, Mockito y Testcontainers.
•	Definición de umbral de cobertura: ≥ 80% en servicios y validadores.
________________________________________
3. Beneficios obtenidos con GPT-5
•	Agilidad: Reducción significativa del tiempo de documentación y configuración.
•	Buenas prácticas: Recomendaciones sobre seguridad (HTTPS, CORS, JWT), validaciones y separación de responsabilidades.
•	Despliegue rápido: Generación de scripts listos para usar en Docker y Render.
•	Pruebas más sólidas: Creación de suites de Postman y estrategias de pruebas automáticas.
________________________________________
4. Limitaciones y validaciones necesarias
Aunque GPT-5 resultó de gran ayuda, fue necesario:
•	Validar manualmente configuraciones sensibles (ej. variables de entorno, credenciales, puertos).
•	Ajustar los ejemplos de código a la realidad del entorno local y productivo.
•	Probar en entornos reales los despliegues (Render/GCP) para asegurar compatibilidad.
________________________________________
5. Conclusión
El uso de GPT-5 en el proyecto Tokenización App permitió:
•	Acelerar el desarrollo técnico.
•	Documentar de forma clara y estructurada.
•	Implementar un pipeline de calidad con pruebas, Docker y despliegue en la nube.
En conclusión, GPT-5 fue utilizado como un asistente de ingeniería que complementó el conocimiento del desarrollador, facilitando la entrega de un proyecto más robusto, documentado y listo para producción

