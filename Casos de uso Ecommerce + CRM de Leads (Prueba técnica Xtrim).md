# Casos de uso: Ecommerce + CRM de Leads

*Prueba técnica Xtrim, Administrador eCommerce. Versión 2, integra los requisitos del PDF de la prueba.*

## 0. Alcance y decisiones generales

**Objetivo.** Un mini sistema que muestra productos y servicios en un ecommerce, captura leads a partir de la actividad del visitante, los gestiona en un CRM, cumple con SEO técnico y está desplegado en la nube.

**Decisiones de diseño**

- **Catálogo unificado.** Productos y servicios son una sola entidad (`CatalogItem`) con un campo `type` (`PRODUCT` o `SERVICE`). Todo caso de uso del catálogo aplica a ambos tipos salvo que se indique lo contrario.
- **Stock opcional.** `stock = NULL` significa que no se controla stock (válido para servicios y también para productos). Solo `stock = 0` se considera «sin stock».
- **Compra simulada.** El checkout no procesa pagos: registra el pedido, descuenta stock y captura los datos del cliente como lead.
- **Visitante anónimo.** El cliente no necesita registrarse. Se genera un UUID (`anonymous_id`) en `localStorage` y toda su actividad se guarda con ese identificador. Cuando deja su contacto, esa actividad se asocia a su lead.
- **Imágenes por URL.** No hay subida de archivos.
- **Dos registros distintos.** `activity_events` guarda el comportamiento del cliente (alimenta el CRM). Las tablas de historial de Hibernate Envers guardan los cambios sobre las tablas auditadas (las consulta el auditor). No se mezclan.
- **Borrado lógico.** Eliminar un ítem lo marca como eliminado (`deleted_at`) para conservar el historial de auditoría y de pedidos.
- **Paginación** obligatoria en todos los listados.
- **Roles:** Visitante (anónimo), Administrador, Auditor.

---

## 1. Ecommerce público

### HU-01. Visualización de la página de inicio

Yo como visitante quiero que la página inicial me muestre:

- productos destacados y más comprados,
- servicios destacados y más populares,
- ítems con ofertas vigentes,

para encontrar rápido lo más relevante.

**Criterios de aceptación**

- «Destacados» depende de un flag `is_featured` que el administrador puede marcar.
- «Más comprados» se calcula a partir de los pedidos confirmados.
- «Ofertas» son los ítems con un descuento vigente.
- Solo se muestran ítems activos y no eliminados.

### HU-02. Búsqueda de producto o servicio

Yo como visitante quiero buscar productos o servicios desde una barra de búsqueda y ver resultados que coincidan con la palabra buscada, y también poder filtrar por categoría.

**Criterios de aceptación**

- La búsqueda coincide por nombre y descripción (sin distinguir mayúsculas ni tildes).
- Se puede combinar búsqueda de texto con filtro de categoría.
- Los resultados están paginados.
- Cada búsqueda enviada se registra como evento `SEARCH` (ver HU-14).

### HU-03. Listado de servicios

Yo como visitante quiero una página que muestre solo el listado de servicios, con los más vendidos y los que tienen oferta, y poder ordenarlos por campos.

**Criterios de aceptación**

- Orden por precio (más baratos o más caros), nombre y más vendidos.
- Filtro «solo con oferta».
- Paginado.
- Es la misma vista del catálogo con el filtro `type = SERVICE`.

### HU-04. Listado de productos

Yo como visitante quiero una página que muestre solo el listado de productos, con los más vendidos y los que tienen oferta, y poder ordenarlos por campos.

**Criterios de aceptación**

- Orden por precio (más baratos o más caros), nombre y más vendidos.
- Filtros: «solo con oferta» y «poco stock» (umbral configurable, por ejemplo 5 unidades o menos).
- Paginado.
- Es la misma vista del catálogo con el filtro `type = PRODUCT`.

### HU-05. Detalle de producto o servicio

Yo como visitante quiero, al hacer clic en un ítem, ver su detalle completo.

**Criterios de aceptación**

- Se muestran nombre, descripción, galería de fotos (URLs), precio (con el precio original tachado si hay descuento vigente) y categorías.
- Las especificaciones se muestran en una tabla de clave y valor.
- Botones de acción:
  - **Agregar al carrito.**
  - **Comprar ahora** (agrega al carrito y va al carrito).
  - **Solicitar más información** (formulario que crea un lead, ver HU-12).
  - **Contactar por WhatsApp** (enlace externo con mensaje precargado sobre el ítem). Este es el CTA que pide la prueba.
- Si el ítem tiene stock `0`, los botones de compra se deshabilitan y aparece HU-08.
- Al abrir el detalle se registra el evento `PRODUCT_VIEW`.
- La URL usa un slug amigable (ver HU-26).

---

## 2. Carrito y compra simulada

### HU-06. Carrito

Yo como visitante quiero agregar productos y servicios a mi carrito, modificar cantidades y eliminar ítems.

**Criterios de aceptación**

- El carrito vive en `localStorage`; no requiere login.
- No se puede pedir una cantidad mayor al stock disponible (cuando el stock se controla).
- Se muestran subtotales y total con descuentos aplicados.
- Se registran los eventos `ADD_TO_CART` y `REMOVE_FROM_CART`.
- Desde el carrito puedo **solicitar información** sobre los ítems (HU-12).

### HU-07. Confirmación de compra (simulada)

Yo como visitante quiero confirmar mi compra llenando un formulario con mis datos.

**Criterios de aceptación**

- El formulario pide nombre, correo, teléfono, provincia y un checkbox de consentimiento («acepto que me contacten»).
- Al confirmar, en una sola transacción:
  1. se crea el pedido (`orders` y `order_items`) con los precios vigentes al momento,
  2. se descuenta el stock de forma segura: se bloquea la fila (`PESSIMISTIC_WRITE`) y se descuenta a través de la entidad, para que el cambio quede en el historial de auditoría,
  3. se crea o actualiza el lead por correo y se le asocia el pedido,
  4. se registra el evento `PURCHASE`.
- Si algún ítem ya no tiene stock suficiente, se rechaza con un mensaje claro.
- No hay pasarela de pago real.
- Al terminar se muestra una pantalla de confirmación.

### HU-08. Suscripción a productos sin stock

Yo como visitante, cuando un ítem tiene stock `0`, quiero un formulario con el checkbox «Notificarme cuando exista stock» para recibir un aviso cuando vuelva a haber.

**Criterios de aceptación**

- Pide correo y consentimiento; crea o actualiza el lead con `source = STOCK_NOTIFY`.
- Se registra el evento `STOCK_SUBSCRIBE`.
- El checkbox solo aparece cuando `stock = 0`, no cuando `stock = NULL`.
- Cuando el stock pasa de `0` a un valor mayor que `0`, las suscripciones pendientes se marcan para notificar. El canal puede ser un correo simulado (se escribe en consola o en una tabla `notifications`).
- Una suscripción por lead e ítem; después de notificar queda marcada como atendida.

---

## 3. Captura de leads y tracking de actividad

### HU-09. Registro de actividad anónima

Yo como sistema quiero registrar las acciones del visitante sin que tenga que loguearse, para construir su historial.

**Criterios de aceptación**

- Al primer ingreso se genera un UUID y se guarda en `localStorage`.
- Eventos registrados: `SEARCH`, `PRODUCT_VIEW`, `ADD_TO_CART`, `REMOVE_FROM_CART`, `PURCHASE`, `STOCK_SUBSCRIBE`, `INFO_REQUEST`. No se registra una vista de página genérica, para evitar ruido.
- Cada evento guarda `anonymous_id`, `lead_id` (nulo mientras sea anónimo), tipo, entidad relacionada, `metadata` en JSON y fecha.
- El envío es «dispara y olvida»: si falla, no interrumpe la navegación del usuario.
- La búsqueda se registra al enviarla (o con debounce), no por cada tecla.

### HU-10. Asociación de actividad anónima a un lead

Yo como sistema quiero que, cuando un visitante deja su contacto, toda su actividad anterior quede asociada a su lead.

**Criterios de aceptación**

- Al capturar un contacto se hace upsert del lead por correo y se ejecuta `UPDATE activity_events SET lead_id = X WHERE anonymous_id = Y AND lead_id IS NULL`.
- La relación `anonymous_id` → `lead_id` se guarda en `visitors`, así los eventos posteriores del mismo navegador ya llegan asociados.
- Si el mismo correo llega desde otro dispositivo, el lead se reutiliza y queda con varios `anonymous_id`.

### HU-11. Captura de lead desde múltiples puntos

Yo como negocio quiero que cualquier formulario donde el cliente deje su contacto genere un lead con su origen.

**Criterios de aceptación**

- Orígenes (`source`): `CHECKOUT`, `STOCK_NOTIFY`, `INFO_REQUEST`, `WHATSAPP`, `MANUAL` (creado desde el CRM).
- Todos los formularios incluyen consentimiento explícito.
- Un correo duplicado no crea un lead nuevo: actualiza el existente y conserva todos sus orígenes.

### HU-12. Solicitar más información

Yo como visitante quiero pedir más información sobre un ítem o sobre mi carrito dejando mis datos.

**Criterios de aceptación**

- Formulario con nombre, correo, teléfono, provincia, mensaje opcional y consentimiento.
- Crea o actualiza el lead con `source = INFO_REQUEST` y guarda los ítems consultados.
- Se registra el evento `INFO_REQUEST`.

---

## 4. Autenticación

### HU-13. Login del personal

Yo como administrador o auditor quiero iniciar sesión para acceder al panel.

**Criterios de aceptación**

- Login con correo y contraseña; se emite un JWT con el rol.
- Rutas del panel protegidas por rol: `ADMIN` accede a catálogo y CRM; `AUDITOR` accede solo a la auditoría, en modo lectura.
- Contraseñas almacenadas con hash.
- Los usuarios iniciales se crean con un seed.

---

## 5. Administración del catálogo

### HU-14. Listado unificado de productos y servicios

Yo como administrador quiero una pantalla con el listado unificado de productos y servicios.

**Criterios de aceptación**

- Filtro por tipo (todos, productos, servicios) y por estado (activos, inactivos, eliminados).
- Búsqueda por palabra, con la misma lógica que la búsqueda pública.
- Orden por `fecha_creacion`, `stock`, `descuento`, `precio` y `nombre`.
- Paginado.

### HU-15. Crear un producto o servicio

Yo como administrador quiero crear un ítem del catálogo.

**Criterios de aceptación**

- Se elige el tipo (producto o servicio).
- Campos: nombre, descripción, precio, stock (opcional), URLs de imágenes, especificaciones (pares clave y valor dinámicos), una o varias categorías y flag `is_featured`.
- Se genera un slug único a partir del nombre.
- Validaciones: nombre obligatorio, precio mayor o igual a 0, stock entero mayor o igual a 0 si se informa.

### HU-16. Editar un producto o servicio

Yo como administrador quiero editar un ítem existente (precio, nombre, descripción, stock, imágenes, especificaciones, categorías y destacado).

**Criterios de aceptación**

- Los cambios quedan registrados automáticamente en el historial de auditoría (Hibernate Envers), con el estado completo del registro en cada versión.
- Si cambia el nombre, el slug anterior redirige al nuevo (o se conserva el slug original; se documenta la decisión).
- Si el stock pasa de `0` a mayor que `0`, se disparan las notificaciones de HU-08.

### HU-17. Inactivar y reactivar un producto o servicio

Yo como administrador quiero inactivar un ítem sin perderlo, y poder reactivarlo y modificarlo en cualquier momento.

**Criterios de aceptación**

- Un ítem inactivo no aparece en el catálogo público, el detalle responde 404 (o «no disponible») y no se puede agregar al carrito.
- Un ítem inactivo sigue visible y editable en el panel.
- Reactivarlo lo devuelve al catálogo.
- Un ítem inactivo que ya estaba en un carrito se informa como no disponible al intentar comprar.

### HU-18. Eliminar un producto o servicio

Yo como administrador quiero eliminar un ítem existente.

**Criterios de aceptación**

- Es un borrado lógico (`deleted_at`); el historial de pedidos y de auditoría se conserva.
- Pide confirmación antes de eliminar.
- Queda registrado en el historial de auditoría como un cambio de `deleted_at`.

### HU-19. Descuentos sobre productos y servicios

Yo como administrador quiero agregar descuentos a un ítem, indefinidos o con fecha de fin, y poder revocarlos manualmente.

**Criterios de aceptación**

- El descuento es un **porcentaje** (0 a 100).
- Fecha de inicio opcional y fecha de fin opcional (sin fecha de fin = indefinido).
- El precio final se calcula al consultar, comparando con la fecha actual; no se necesita un proceso programado para expirarlos.
- Revocar un descuento lo desactiva de inmediato.
- Solo un descuento vigente por ítem a la vez.
- Crear y revocar quedan registrados en el historial de auditoría de `discounts`.

### HU-20. Gestión de categorías

Yo como administrador quiero crear, editar y eliminar categorías para asignarlas a los ítems.

**Criterios de aceptación**

- Campos: nombre y slug único.
- No se puede eliminar una categoría con ítems asignados (o se pide reasignarlos).
- Opcional: los cambios se auditan anotando la entidad `Category` con `@Audited`.

---

## 6. CRM de leads

### HU-21. CRUD de leads

Yo como administrador quiero crear, editar, listar y eliminar leads.

**Criterios de aceptación**

- Campos mínimos: **nombre, teléfono, provincia y estado**. Adicionales: correo, origen (`source`), monto de venta en $, consentimiento y fecha de creación.
- Provincia se elige de la lista de provincias de Ecuador.
- Los leads creados desde el ecommerce aparecen automáticamente con estado `Nuevo`.
- Se puede crear un lead manualmente (`source = MANUAL`).
- El listado tiene búsqueda (nombre, correo, teléfono), filtros por estado, provincia y origen, y paginación.
- Eliminar pide confirmación.

### HU-22. Flujo de estados

Yo como administrador quiero cambiar el estado de un lead dentro de un flujo definido.

**Criterios de aceptación**

- Estados: **Nuevo → En gestión → Cerrado**.
- Transiciones permitidas: `Nuevo → En gestión`, `En gestión → Cerrado`, y reabrir (`Cerrado → En gestión`). No se puede saltar de `Nuevo` a `Cerrado`; el backend valida la transición, no solo el frontend.
- Al pasar a `Cerrado` se registra el resultado (ganado o perdido) y, si es ganado, el monto de la venta en $.
- Cada cambio de estado se guarda en un historial (usuario, estado anterior, estado nuevo, fecha).

### HU-23. Pipeline en vista de lista y Kanban

Yo como administrador quiero ver los leads como lista y como tablero Kanban.

**Criterios de aceptación**

- Se puede alternar entre vista de lista y vista Kanban.
- El Kanban tiene una columna por estado, con el conteo y el monto acumulado de cada columna.
- Arrastrar una tarjeta a otra columna cambia el estado, respetando las reglas de HU-22; si la transición no es válida, la tarjeta vuelve a su lugar con un mensaje.
- Alternativa si el tiempo no alcanza: un botón o menú «Cambiar estado» en cada tarjeta.

### HU-24. KPI de ventas

Yo como administrador quiero ver indicadores de ventas en dólares.

**Criterios de aceptación**

- Ventas cerradas en $ (suma de leads cerrados como ganados).
- Cantidad de leads por estado.
- Tasa de conversión (cerrados ganados sobre total de leads).
- Monto potencial en gestión (suma de montos de leads en estado `En gestión`).
- Filtro por rango de fechas.

### HU-25. Detalle de lead y línea de tiempo

Yo como administrador quiero abrir un lead y ver todo lo que hizo antes de dejar su contacto.

**Criterios de aceptación**

- Datos del lead, origen(es) y estado.
- Línea de tiempo cronológica con su actividad: búsquedas, productos vistos, agregados y retirados del carrito, compras, suscripciones de stock y solicitudes de información.
- Pedidos del lead con sus ítems y montos.
- Notas internas (agregar y listar).
- Historial de cambios de estado.

---

## 7. SEO técnico

### HU-26. URLs amigables

- Rutas con slug: `/productos`, `/productos/{slug}`, `/servicios`, `/servicios/{slug}`, `/categorias/{slug}`.
- Slugs únicos, en minúsculas, sin tildes ni caracteres especiales.

### HU-27. Meta etiquetas dinámicas

- `title` y `description` generados por página: detalle (nombre del ítem y extracto de la descripción), listados, categorías e inicio.
- Etiqueta `canonical` en cada página.
- Open Graph básico (título, descripción e imagen) para compartir en redes.
- Datos estructurados JSON-LD (`Product` / `Service`) en el detalle.

### HU-28. sitemap.xml y robots.txt

- `sitemap.xml` generado dinámicamente con inicio, listados, categorías y todos los ítems activos (sin inactivos ni eliminados).
- `robots.txt` que permite el catálogo, bloquea el panel de administración y el CRM, e indica la ruta del sitemap.
- El panel interno lleva `noindex`.

### HU-29. Renderizado optimizado para SEO

- Las páginas públicas (inicio, listados, detalle) se renderizan en servidor (SSR) o se pre-renderizan, de modo que el HTML inicial ya contenga el contenido.
- El panel de administración y el CRM pueden ser una aplicación de cliente.

### HU-30. Optimización de carga

- Imágenes con `loading="lazy"`, dimensiones declaradas (para evitar saltos de diseño) y formatos livianos.
- Fuentes y recursos optimizados; sin librerías pesadas innecesarias.
- Prioridad mobile first; apuntar a buen puntaje en Lighthouse móvil.

---

## 8. Auditoría

### HU-31. Auditoría de productos y servicios

Yo como auditor quiero revisar todas las transacciones realizadas sobre un producto o servicio.

**Criterios de aceptación**

- Pantalla de solo lectura, con acceso exclusivo para `AUDITOR` y `ADMIN`.
- Se audita con Hibernate Envers: `catalog_items` y `discounts` tienen cada una su tabla de historial (`catalog_items_history` y `discounts_history`) y una tabla común `revinfo` con la fecha y el usuario de cada cambio.
- Cada versión guarda el estado completo del registro, el tipo de operación (alta, modificación o baja) y quién la hizo.
- La pantalla muestra la línea de versiones de un ítem y resalta los campos que cambiaron respecto de la versión anterior.
- Los cambios hechos por compras (descuento de stock) aparecen con el usuario `SISTEMA`, porque el comprador es anónimo.
- Filtros por ítem, usuario, operación y rango de fechas; paginado.
- El historial no se edita ni se borra desde la aplicación.
- Limitación conocida: Envers solo registra cambios hechos a través de JPA; un `UPDATE` directo en SQL no queda en el historial (ver sección 12.8).

---

## 9. Despliegue y entregables

### HU-32. Publicación en la nube

- Sistema desplegado en una plataforma cloud con URL pública funcional (frontend, backend y base de datos).
- Variables de entorno fuera del repositorio.
- Seed con datos de ejemplo (productos, servicios, categorías, leads y usuarios de prueba).

### HU-33. Documentación y respuestas

- **README** con instrucciones para ejecutar el proyecto, explicación general, decisiones técnicas (incluye catálogo unificado, tracking anónimo, soft delete y reglas del flujo de estados), usuarios de prueba y la URL del demo.
- **Respuestas del caso práctico** (caída de tráfico orgánico del 40%, formulario intermitente y lentitud en móviles): qué revisar primero, cómo diagnosticar y posibles soluciones.
- **Respuesta de escalabilidad** (de 100 a 10.000 leads diarios).
- Sección de «siguientes pasos» con lo que quedó fuera de alcance.
- Opcional: video corto explicando la solución.

---

## 10. Priorización

La prueba pide entregar en 24 a 48 horas y permite priorizar. Orden sugerido:

**Imprescindible (lo que la prueba pide de forma explícita)**

- HU-21 a HU-23 (CRM con CRUD, estados, lista y Kanban) y HU-24 (KPI de ventas).
- HU-02 a HU-05 (listado, búsqueda y detalle con CTA).
- HU-26 a HU-29 (SEO: URLs, meta etiquetas, sitemap, robots y SSR).
- HU-32 y HU-33 (despliegue, README y caso práctico).
- HU-13 y HU-15 a HU-17 (login y catálogo administrable).

**Alta prioridad (son lo que diferencia tu solución)**

- HU-06 y HU-07 (carrito y compra simulada).
- HU-09 a HU-12 (tracking anónimo y captura de leads).
- HU-25 (línea de tiempo del lead).

**Si da el tiempo**

- HU-01 (home con destacados y más comprados), HU-18 a HU-20 (eliminar, descuentos, categorías), HU-08 (suscripción de stock), HU-30 (optimización de carga), HU-31 (auditoría).

**Fuera de alcance (documentar como siguientes pasos)**

- Pasarela de pago real, envío real de correos, multiusuario avanzado en el CRM y reportes exportables.

---

## 11. Modelo de datos (resumen)

- `users`: id, email, password\_hash, role (`ADMIN` | `AUDITOR`).
- `catalog_items`: id, type, name, slug, description, price, stock (nullable), images (JSON), specs (JSON), is\_featured, is\_active, deleted\_at, created\_at.
- `categories` y `catalog_item_categories` (relación muchos a muchos).
- `discounts`: id, item\_id, percentage, starts\_at, ends\_at (nullable), revoked\_at (nullable).
- `leads`: id, name, email (único), phone, province, status, source(s), sale\_amount, consent, created\_at.
- `lead_status_history` y `lead_notes`.
- `visitors`: anonymous\_id, lead\_id (nullable).
- `activity_events`: id, anonymous\_id, lead\_id (nullable), event\_type, entity\_type, entity\_id, metadata (JSON), created\_at.
- `orders` y `order_items`: pedido con lead, total y precios al momento de la compra.
- `stock_subscriptions`: id, lead\_id, item\_id, notified\_at (nullable).
- Auditoría (Hibernate Envers): `revinfo` (id, timestamp, username) y las tablas de historial `catalog_items_history`, `discounts_history` y `catalog_item_categories_history`. Cada una repite las columnas de su tabla original más `rev` (versión) y `revtype` (0 alta, 1 modificación, 2 baja).

---

## 12. Arquitectura técnica (Angular + Spring Boot)

*Esta sección define cómo se organiza el código, qué contratos (interfaces) existen y cómo se aplica SOLID. Con el plazo corto, solo se crea una interfaz donde hay variación real o donde ayuda a probar.*

### 12.1 Stack y decisiones

- **Backend:** Spring Boot 3 (Java 17+), Spring Web, Spring Data JPA, Spring Security con JWT, Bean Validation, Flyway (migraciones), PostgreSQL y springdoc-openapi (Swagger).
- **Frontend:** Angular 17 o superior con `@angular/ssr` (el SSR es obligatorio en la prueba), componentes standalone y los servicios `Title` y `Meta` de Angular para el SEO.
- **Despliegue:** el SSR de Angular necesita un servidor Node, así que son tres piezas: frontend SSR (Node), API Spring Boot (contenedor Docker) y PostgreSQL administrado. Render, Railway o Fly.io permiten las tres en un solo lugar; Vercel no hospeda Spring Boot.
- **sitemap.xml y robots.txt:** se sirven desde el servidor Express del SSR (`server.ts`), que consulta a la API la lista de slugs activos.
- **Repositorios:** los repositorios Spring Data ya son interfaces, se usan directamente (sin una capa extra de puertos). Los servicios de aplicación sí tienen interfaz.
- **Repositorio de código:** un solo repo con las carpetas `backend/` y `frontend/`.

### 12.2 Estructura del backend

Se organiza por funcionalidad (no por tipo técnico) y, dentro de cada una, en capas con las dependencias hacia adentro.

```
com.xtrim.shop
├─ catalog/   api · application · domain · infrastructure
├─ order/
├─ lead/
├─ tracking/
├─ audit/
├─ auth/
└─ shared/    errores, paginación, configuración, seguridad
```

- `api`: controllers y DTOs. Solo HTTP, sin lógica de negocio.
- `application`: interfaces de servicios (los contratos) y sus implementaciones (casos de uso).
- `domain`: entidades, enums y políticas puras (precio, transiciones), sin Spring ni I/O.
- `infrastructure`: adaptadores (notificaciones, JWT) y repositorios.

Regla de dependencias: `api → application → domain`. `infrastructure` implementa las interfaces de `application` y `domain`. El dominio no depende de nada.

### 12.3 SOLID: dónde se aplica

| Principio | Aplicación concreta |
| --- | --- |
| **S** (responsabilidad única) | El controller solo traduce HTTP a DTO y viceversa. `CatalogCommandService` escribe y `CatalogQueryService` lee. `PricingPolicy` solo calcula precios. Envers (`@Audited`) se encarga de la auditoría y `AuditQueryService` solo la consulta. |
| **O** (abierto/cerrado) | Un canal de notificación nuevo es una nueva implementación de `StockNotificationSender`. Un criterio de orden nuevo es una entrada más en el registro de ordenamientos, sin tocar el servicio. Un tipo de descuento nuevo es otra implementación de `PricingPolicy`. |
| **L** (sustitución de Liskov) | Toda implementación respeta el contrato documentado. Por ejemplo, ningún `ActivityTracker` lanza excepciones al llamador y todo `StockNotificationSender` es idempotente por suscripción. |
| **I** (segregación de interfaces) | Interfaces pequeñas por rol: lectura y escritura separadas (`LeadQueryService` y `LeadCommandService`), y `ActivityTracker` expone solo `track`. Nadie depende de métodos que no usa. |
| **D** (inversión de dependencias) | Los servicios reciben sus dependencias por constructor y como interfaz (`StockService`, `PricingPolicy`, `StockNotificationSender`). Las pruebas unitarias usan dobles sin levantar Spring. |

### 12.4 Contratos del backend (interfaces)

Convención: cada método documenta precondiciones, postcondiciones y excepciones en Javadoc. Las excepciones de negocio extienden `BusinessException` y un `@RestControllerAdvice` las traduce a HTTP.

#### Catálogo

```java
public interface CatalogQueryService {
    /** Público: solo ítems activos y no eliminados. Paginado. Nunca null. */
    PageResponse<CatalogItemSummary> search(CatalogSearchCriteria criteria, PageRequestDto page);

    /** @throws NotFoundException si el slug no existe, está inactivo o eliminado. */
    CatalogItemDetail getPublicBySlug(String slug);

    HomeContent getHome();            // destacados, más comprados y ofertas
    List<String> listActiveSlugs();   // para el sitemap
}

public interface CatalogAdminQueryService {
    /** Incluye inactivos y, si se pide, eliminados. */
    PageResponse<CatalogItemAdminView> search(AdminCatalogCriteria criteria, PageRequestDto page);
}

public interface CatalogCommandService {
    /** Post: slug único generado. @throws ValidationException si precio < 0 o stock < 0. */
    CatalogItemDetail create(CreateCatalogItemCommand cmd);

    /** Envers registra la versión (@Audited). Si el stock pasa de 0 a > 0, notifica suscriptores. */
    CatalogItemDetail update(Long id, UpdateCatalogItemCommand cmd);

    void deactivate(Long id);   // reversible
    void reactivate(Long id);
    void delete(Long id);       // borrado lógico

    /** Solo un descuento vigente por ítem. */
    void applyDiscount(Long id, ApplyDiscountCommand cmd);
    void revokeDiscount(Long id);
}
```

#### Precio y stock

```java
public interface PricingPolicy {
    /**
     * Pura: sin I/O. Nunca devuelve null y finalPrice >= 0.
     * Un descuento está vigente si no fue revocado y now está entre su inicio
     * y su fin (fin nulo = indefinido). El parámetro discount puede ser null.
     */
    PriceBreakdown calculate(BigDecimal basePrice, Discount discount, Instant now);
}

public interface StockService {
    /**
     * Atómico: bloqueo pesimista y descuento vía entidad (Envers no audita un UPDATE directo). No hace nada si el stock es null
     * (no controlado). @throws InsufficientStockException
     */
    void decrease(Long itemId, int qty, String reference);

    /** Publica StockReplenishedEvent cuando el stock pasa de 0 a > 0. */
    void setStock(Long itemId, int newStock, String reason);
}
```

#### Compra

```java
public interface CheckoutService {
    /**
     * Transaccional (todo o nada): crea el pedido, descuenta stock, captura el lead
     * y registra el evento PURCHASE. Usa siempre el precio vigente del servidor,
     * nunca el enviado por el cliente.
     * @throws InsufficientStockException, NotFoundException (ítem inactivo o eliminado)
     */
    OrderConfirmation checkout(CheckoutCommand cmd);
}
```

#### Leads

```java
public enum LeadStatus { NEW, IN_PROGRESS, CLOSED }   // Nuevo, En gestión, Cerrado

public interface LeadCaptureService {
    /**
     * Idempotente por email: crea o actualiza y agrega la fuente si es nueva.
     * Post: la actividad previa de anonymousId queda asociada al lead.
     * @throws ValidationException si falta el consentimiento.
     */
    LeadView capture(CaptureLeadCommand cmd);
}

public interface LeadCommandService {
    LeadView create(CreateLeadCommand cmd);
    LeadView update(Long id, UpdateLeadCommand cmd);

    /** Valida con LeadStatusPolicy. @throws InvalidStatusTransitionException */
    LeadView changeStatus(Long id, ChangeStatusCommand cmd);

    void delete(Long id);
    NoteView addNote(Long id, String text);
}

public interface LeadQueryService {
    PageResponse<LeadView> search(LeadSearchCriteria criteria, PageRequestDto page);
    LeadDetail getDetail(Long id);                  // incluye timeline, pedidos y notas
    KanbanBoard getKanban(LeadSearchCriteria c);    // columnas con conteo y monto
}

public interface LeadKpiService {
    /** Ventas cerradas en $, tasa de conversión y monto en gestión. */
    LeadKpis calculate(DateRange range);
}

public interface LeadStatusPolicy {
    /** Permitidas: NEW a IN_PROGRESS, IN_PROGRESS a CLOSED y CLOSED a IN_PROGRESS (reabrir). */
    boolean canTransition(LeadStatus from, LeadStatus to);

    /** @throws InvalidStatusTransitionException si no está permitida. */
    void assertAllowed(LeadStatus from, LeadStatus to);
}
```

#### Tracking y notificaciones de stock

```java
public interface ActivityTracker {
    /** Best-effort y asíncrono: NUNCA lanza una excepción al llamador. */
    void track(TrackEventCommand cmd);
}

public interface VisitorLinker {
    /** Asocia el visitante y sus eventos con lead_id nulo. @return eventos actualizados. */
    int link(String anonymousId, Long leadId);
}

public interface StockSubscriptionService {
    /** Crea el lead (fuente STOCK_NOTIFY) y la suscripción. Una por lead e ítem. */
    void subscribe(SubscribeCommand cmd);
}

public interface StockNotificationSender {
    /** Idempotente por suscripción. Implementaciones: LogSender (consola) y EmailSender (futuro). */
    void send(StockNotification notification);
}
```

Un listener de `StockReplenishedEvent` (`StockSubscriptionNotifier`) busca las suscripciones pendientes y llama a `StockNotificationSender`.

#### Auditoría y seguridad

```java
public interface AuditQueryService {
    /** Solo lectura. Versiones de catalog_items y discounts, de la más reciente a la más antigua. */
    PageResponse<AuditVersionView> search(AuditCriteria criteria, PageRequestDto page);
}

public interface TokenService {
    String issue(AuthenticatedUser user);
    AuthenticatedUser parse(String token);   // @throws InvalidTokenException
}

public interface AuthService {
    LoginResponse login(LoginRequest request);   // @throws BadCredentialsException
}
```

### 12.5 Contrato REST

Prefijos: `/api/public` (sin autenticación), `/api/auth` y `/api/admin` (JWT con el rol indicado). Se documenta con OpenAPI (springdoc) en `/swagger-ui`.

| Método | Ruta | Rol | Descripción |
| --- | --- | --- | --- |
| GET | `/api/public/home` | Público | Destacados, más comprados y ofertas |
| GET | `/api/public/catalog` | Público | Listado. Query: `type`, `q`, `category`, `sort`, `onSale`, `lowStock`, `page`, `size` |
| GET | `/api/public/catalog/{slug}` | Público | Detalle de un ítem |
| GET | `/api/public/catalog/slugs` | Público | Slugs activos (para el sitemap) |
| GET | `/api/public/categories` | Público | Categorías |
| POST | `/api/public/events` | Público | Registrar un evento (responde 202) |
| POST | `/api/public/info-requests` | Público | Solicitud de información (crea el lead) |
| POST | `/api/public/stock-subscriptions` | Público | Suscripción de stock |
| POST | `/api/public/orders` | Público | Checkout simulado |
| POST | `/api/auth/login` | Público | Devuelve el JWT |
| GET, POST | `/api/admin/catalog` | ADMIN | Listado unificado y crear |
| PUT, DELETE | `/api/admin/catalog/{id}` | ADMIN | Editar y eliminar (lógico) |
| PATCH | `/api/admin/catalog/{id}/active` | ADMIN | Inactivar o reactivar |
| PUT, DELETE | `/api/admin/catalog/{id}/discount` | ADMIN | Aplicar y revocar descuento |
| GET, POST, PUT, DELETE | `/api/admin/categories` | ADMIN | CRUD de categorías |
| GET, POST | `/api/admin/leads` | ADMIN | Listar y crear leads |
| GET, PUT, DELETE | `/api/admin/leads/{id}` | ADMIN | Detalle, editar y eliminar |
| PATCH | `/api/admin/leads/{id}/status` | ADMIN | Cambiar estado |
| POST | `/api/admin/leads/{id}/notes` | ADMIN | Agregar nota |
| GET | `/api/admin/leads/kanban` | ADMIN | Tablero Kanban |
| GET | `/api/admin/leads/kpis` | ADMIN | KPI de ventas |
| GET | `/api/admin/audit` | ADMIN, AUDITOR | Auditoría (solo lectura) |

**Convenciones**

- **Paginación:** parámetros `page` (desde 0) y `size` (máximo 50). La respuesta siempre tiene la forma `{ content, page, size, totalElements, totalPages }`.
- **Errores:** un único formato en toda la API.

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "No hay stock suficiente para el ítem solicitado",
  "details": [{ "field": "items[0].quantity", "issue": "max 3" }],
  "timestamp": "2026-10-20T09:30:00Z"
}
```

| HTTP | `code` | Cuándo |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Datos inválidos o falta de consentimiento |
| 401 / 403 | `UNAUTHORIZED` / `FORBIDDEN` | Sin token, token inválido o rol insuficiente |
| 404 | `NOT_FOUND` | Ítem, lead o slug inexistente o no disponible |
| 409 | `INSUFFICIENT_STOCK` | Compra con stock insuficiente |
| 409 | `INVALID_STATUS_TRANSITION` | Cambio de estado no permitido |

**Ejemplos de DTO**

```json
// POST /api/public/events
{
  "anonymousId": "3f0c9b52-6a1e-4d8e-9c3a-2b7d5e1f8a10",
  "type": "PRODUCT_VIEW",
  "entityType": "CATALOG_ITEM",
  "entityId": 12,
  "metadata": { "query": "internet hogar" }
}

// POST /api/public/orders
{
  "anonymousId": "3f0c9b52-6a1e-4d8e-9c3a-2b7d5e1f8a10",
  "customer": { "name": "Ana Pérez", "email": "ana@mail.com", "phone": "0999999999", "province": "Pichincha", "consent": true },
  "items": [{ "itemId": 12, "quantity": 2 }]
}
```

En el checkout el cliente envía solo `itemId` y `quantity`; el precio lo calcula el servidor.

### 12.6 Frontend Angular

```
src/app
├─ core/      auth, http (interceptors), storage, tracking, seo
├─ shared/    componentes UI, pipes y modelos (interfaces TS)
└─ features/  catalog · cart · checkout · admin (catalog, leads, audit)
```

- Componentes de presentación (solo `@Input` y `@Output`) separados de los contenedores, que orquestan servicios.
- Toda llamada HTTP vive en servicios `*Api`, nunca en los componentes.
- Los modelos TypeScript reflejan los DTO del backend.
- Interceptores: `authInterceptor` (agrega el JWT) y `errorInterceptor` (interpreta el formato de error). Guards: `authGuard` y `roleGuard`.
- Las rutas públicas se renderizan en servidor. `/admin/**` se renderiza solo en cliente y lleva `noindex`.

**SOLID en el frontend (inversión de dependencias con SSR).** `localStorage` no existe en el servidor, así que se abstrae con una clase abstracta que Angular usa como token de inyección:

```ts
export abstract class StoragePort {
  abstract get(key: string): string | null;
  abstract set(key: string, value: string): void;
  abstract remove(key: string): void;
}

@Injectable()
export class BrowserStorage extends StoragePort { /* usa window.localStorage */ }

@Injectable()
export class ServerStorage extends StoragePort { /* en memoria o no-op */ }

// app.config.ts:        { provide: StoragePort, useClass: BrowserStorage }
// app.config.server.ts: { provide: StoragePort, useClass: ServerStorage }
```

`CartService` y `AnonymousIdProvider` dependen de `StoragePort`, no de `localStorage`.

**Contratos de los servicios**

```ts
export abstract class CatalogApi {
  abstract search(criteria: CatalogSearchCriteria): Observable<Page<CatalogItemSummary>>;
  abstract getBySlug(slug: string): Observable<CatalogItemDetail>;
}

export abstract class Tracker {
  /** Fire-and-forget: nunca lanza ni bloquea la interfaz. */
  abstract track(type: EventType, entity?: EntityRef, metadata?: Record<string, unknown>): void;
}

export abstract class SeoService {
  /** Título, descripción, canonical, Open Graph y JSON-LD. */
  abstract setPage(meta: PageMeta): void;
}

export abstract class CartStore {
  abstract readonly lines: Signal<CartLine[]>;
  abstract add(itemId: number, quantity: number): void;
  abstract setQuantity(itemId: number, quantity: number): void;
  abstract remove(itemId: number): void;
  abstract clear(): void;
}
```

- `Tracker` captura los errores (`catchError` devuelve vacío), no espera la respuesta y solo envía desde el navegador (no desde el servidor).
- `SeoService.setPage` se llama desde el contenedor de cada página pública, con los datos ya cargados.

### 12.7 Dónde no forzar SOLID

- No crear interfaces para mappers, DTOs ni repositorios Spring Data.
- Una interfaz solo se justifica si hay variación real o si facilita probar; las de arriba cumplen al menos una de las dos.
- Pruebas con mejor retorno por tiempo: `PricingPolicy` y `LeadStatusPolicy` (puras, unos 15 minutos las dos), `StockService` (descuento atómico), `CheckoutService` y `VisitorLinker`.

### 12.8 Auditoría con Hibernate Envers

Envers guarda una versión completa de cada fila auditada cada vez que se crea, modifica o borra a través de JPA, así que los servicios no auditan a mano.

**Configuración**

- Dependencia `hibernate-envers` en el `pom.xml`.
- `@Audited` en las entidades `CatalogItem` y `Discount`. La relación de `CatalogItem` con `Category` lleva `@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)`: se audita la tabla de unión sin auditar las categorías.
- Sufijo de las tablas de historial: `org.hibernate.envers.audit_table_suffix=_history` (por defecto sería `_aud`).
- `org.hibernate.envers.store_data_at_delete=true` para conservar el último estado si algún día se borra físicamente un registro.
- Con Flyway, las tablas de historial se crean en una migración (Hibernate solo las valida). Cada una repite las columnas de la original más `rev` y `revtype`.

**Quién hizo el cambio.** Una entidad de revisión propia guarda el usuario en `revinfo`. Los cambios sin usuario autenticado (las compras) quedan como `SISTEMA`.

```java
@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision extends DefaultRevisionEntity {
    private String username;   // getter y setter omitidos
}

public class AuditRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean staff = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
        ((AuditRevision) revisionEntity).setUsername(staff ? auth.getName() : "SISTEMA");
    }
}
```

**Consulta.** `AuditQueryService` usa `AuditReaderFactory.get(entityManager)` y `createQuery().forRevisionsOfEntity(CatalogItem.class, false, true)`, que devuelve cada versión con su revisión (usuario y fecha) y el tipo de operación. El «antes y después» se calcula comparando versiones consecutivas.

**Reglas para que Envers no pierda cambios**

- Todo cambio pasa por la entidad. No usar `@Modifying`, JPQL de actualización ni SQL nativo sobre tablas auditadas: Envers no los ve.
- Por eso el descuento de stock carga la fila con bloqueo y modifica la entidad:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select i from CatalogItem i where i.id = :id")
Optional<CatalogItem> findByIdForUpdate(@Param("id") Long id);

// StockService.decrease (dentro del @Transactional del checkout):
// 1. item = repo.findByIdForUpdate(id)  -> una compra concurrente espera aquí
// 2. si item.getStock() != null y es menor que qty -> InsufficientStockException
// 3. item.setStock(item.getStock() - qty)  -> al confirmar, Envers crea la versión
```

- El borrado es lógico, así que aparece en el historial como una modificación de `deleted_at`.
- Un `UPDATE` hecho a mano directamente en la base de datos no queda registrado. Es la limitación de Envers frente a los triggers; documéntala en el README como decisión.
- Prueba de integración con mejor retorno: crear un ítem, cambiar su precio y comprarlo, y verificar que el historial tiene 3 versiones con los usuarios correctos (admin, admin y `SISTEMA`).
