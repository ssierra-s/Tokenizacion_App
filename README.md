### Tokenización App (Spring Boot 3 / Java 21)

API REST que tokeniza tarjetas (AES-GCM), maneja carrito y órdenes con reintentos de pago, y envía emails de aprobación/rechazo post-commit. Seguridad por API Key y manejo uniforme de errores (RFC-7807).

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
- baseUrl = http://localhost:8080
- apiKey = dev-123
- userId = 1 (o el que uses)

Orden recomendado de ejecución:
1. Ping
2. Productos - Listar (guarda productId)
3. Carrito - Agregar y Carrito - Ver
4. Tarjetas - Tokenizar (guarda cardToken)
5. Orden - Desde carrito o Orden - con products
6. Orden - Obtener por id

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
