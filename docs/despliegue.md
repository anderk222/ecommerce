# Despliegue

Esta guía cubre el camino recomendado: **Render** con el blueprint que ya está
en el repositorio (`render.yaml`). Al final hay una alternativa para un servidor
propio con Docker Compose.

## Lo primero: el SSR ya está configurado

No hay que agregarlo. El frontend es Angular 22 con renderizado en el servidor:

| Pieza | Archivo | Qué hace |
| --- | --- | --- |
| Entrada del servidor | `frontend/src/main.server.ts` | Arranca Angular en Node |
| Servidor HTTP | `frontend/src/server.ts` | Express: SSR, proxy `/api`, `sitemap.xml`, `robots.txt`, compresión |
| Configuración del servidor | `frontend/src/app/app.config.server.ts` | URL de la API, URL pública, almacenamiento sin `localStorage` |
| Modo de render por ruta | `frontend/src/app/app.routes.server.ts` | Tienda en el servidor; `/admin` solo en el navegador |
| Build | `frontend/angular.json` | `outputMode: server` y entrada `src/server.ts` |

El navegador siempre llama a `/api` en su propio origen; el servidor Node lo
reenvía a la API real. En producción la API se toma de `PUBLIC_API_URL`.

Puedes comprobarlo en local:

```bash
cd frontend
npm ci
npm run build
PUBLIC_API_URL=http://localhost:8080/api \
SITE_BASE_URL=http://localhost:4000 \
NG_ALLOWED_HOSTS=localhost,127.0.0.1 \
node dist/frontend/server/server.mjs
```

La home debe responder `200` y traer el HTML ya renderizado (no un `<app-root>`
vacío).

## Qué se despliega en Render

| Recurso | Tipo | Para qué |
| --- | --- | --- |
| `xtrim-site` | Web service (Docker) | El sitio Angular con SSR y el proxy `/api` |
| `xtrim-api` | Web service (Docker) | La API Spring Boot |
| `xtrim-db` | Render Postgres | La base de datos (Flyway crea y siembra el esquema) |
| `xtrim-redis` | Render Key Value | La caché del catálogo |

`render.yaml` ya declara las cuatro piezas y las variables que las conectan.
Los secretos no están en el repositorio: Render genera `JWT_SECRET` e inyecta
las credenciales de la base.

## Paso 1 — probar la pila completa en local

Es opcional, pero es la mejor forma de no descubrir un fallo en la nube. Usa los
mismos dos `Dockerfile` que Render:

```bash
cp .env.example .env
openssl rand -base64 48        # pega el resultado en JWT_SECRET dentro de .env
scripts/stack.sh up            # construye y espera a que todo esté sano
scripts/stack.sh smoke         # SSR, proxy /api, login, panel, robots y sitemap
```

Si el smoke pasa, el despliegue tiene muy pocas sorpresas.

## Paso 2 — subir el repositorio

`render.yaml` tiene que estar en la raíz del repositorio. Confirma que el árbol
que vas a desplegar está commiteado y empujado. Este checkout todavía no tiene
un remoto configurado, así que el primer paso es crear el repositorio en GitHub
y añadirlo:

```bash
git remote add origin git@github.com:<usuario>/<repo>.git
git branch -M main
git push -u origin main
```

Para los cambios siguientes basta con:

```bash
git status
git add .
git commit -m "deploy: configuración de Render"
git push
```

> El `.env` real nunca se sube: está en `.gitignore`. Render construye desde
> GitHub, no desde tu disco: si un archivo no está commiteado, no llega al
> despliegue.

## Paso 3 — crear el Blueprint

1. Entra a <https://dashboard.render.com>.
2. **New +** → **Blueprint**.
3. Conecta tu cuenta de GitHub y elige el repositorio.
4. Render lee `render.yaml` y muestra los cuatro recursos. Pulsa **Apply**.

Render crea y despliega en orden. La primera construcción tarda varios minutos
(descarga dependencias de Maven y npm). Si un nombre ya existe en tu cuenta,
Render pedirá cambiarlo; las referencias entre servicios se actualizan solas.

> **Primer arranque:** las variables que apuntan a otro servicio
> (`PUBLIC_API_URL`, `CORS_ALLOWED_ORIGINS`) se resuelven cuando el otro
> servicio ya existe. Si el sitio arrancó antes de que la API estuviera lista,
> al terminar el sync haz un **Manual Deploy → Deploy latest commit** del sitio.
> Es un solo clic y solo hace falta la primera vez.

## Paso 4 — revisar las variables

Estas las inyecta el blueprint y no se tocan:

| Variable | Servicio | Valor |
| --- | --- | --- |
| `JWT_SECRET` | API | Generado por Render |
| `SPRING_DATASOURCE_HOST/PORT/DB/USERNAME/PASSWORD` | API | Propiedades de `xtrim-db` |
| `REDIS_HOST` / `REDIS_PORT` | API | Propiedades de `xtrim-redis` |
| `PUBLIC_API_URL` | Sitio | URL pública de `xtrim-api` (el sitio le añade `/api`) |
| `SITE_BASE_URL` | Sitio | URL pública de `xtrim-site` |
| `NG_ALLOWED_HOSTS` | Sitio | Hostname de `xtrim-site` (sin esquema ni puerto) |
| `CORS_ALLOWED_ORIGINS` | API | URL pública de `xtrim-site` |

### Si usas un dominio propio

Cámbialas a mano en el panel de Render y vuelve a desplegar:

```text
SITE_BASE_URL=https://tudominio.com
CORS_ALLOWED_ORIGINS=https://tudominio.com
NG_ALLOWED_HOSTS=tudominio.com
```

`PUBLIC_API_URL` puede seguir apuntando al `onrender.com` de la API o a un
subdominio propio (`https://api.tudominio.com`).

## Paso 5 — verificar

Sustituye `xtrim-site.onrender.com` por la URL que te dio Render:

```bash
BASE=https://xtrim-site.onrender.com

curl -i "$BASE/"                          # 200 + HTML renderizado
curl -s "$BASE/" | grep -o '<title>[^<]*' # el <title> del catálogo
curl -i "$BASE/api/health"                # la API a través del proxy
curl -s "$BASE/robots.txt"                # Disallow: /admin + Sitemap
curl -s "$BASE/sitemap.xml" | head        # <urlset> con productos y servicios
```

Y en el navegador:

- Tienda: `https://xtrim-site.onrender.com`
- Panel: `https://xtrim-site.onrender.com/admin/login` (`admin@xtrim.ec` / `admin123`)

Comprueba también el código fuente de la home (`Ctrl+U`): debe traer el catálogo
en el HTML, no solo el esqueleto de Angular.

## Actualizaciones

Cada `push` a la rama conectada vuelve a desplegar. No hay que tocar el panel.
Si quieres desactivarlo, añade `autoDeployTrigger: 'off'` al servicio en
`render.yaml`.

## Limitaciones del plan `free`

El blueprint usa `plan: free` para no generar cargos. Tenlo en cuenta:

| Límite | Efecto |
| --- | --- |
| Un web service free **duerme a los 15 min** sin tráfico | La primera visita tarda ~1 minuto en despertar |
| Postgres free **caduca a los 30 días** | Hay 14 días de gracia para pasarlo a un plan pago; después se borra |
| Key Value free: 25 MB | Suficiente para esta caché de 5 minutos |
| 0.1 CPU / 512 MB por web service | La API Spring Boot va justa; si ves `502` u `OOM`, súbela |

Para quitar los límites, cambia en `render.yaml`:

```yaml
plan: 1c-2g     # web services (1 CPU, 2 GB)
plan: 0.5c-1g   # Postgres (mínimo pago)
plan: 256mb     # Key Value
```

y sincroniza el blueprint.

## Problemas típicos

| Síntoma | Causa probable | Arreglo |
| --- | --- | --- |
| `Invalid Host header` o `403` al abrir el sitio | `NG_ALLOWED_HOSTS` no incluye el hostname | Añade el hostname (sin `https://` ni puerto) y redespliega |
| `/api/...` responde `502 UPSTREAM_UNAVAILABLE` | `PUBLIC_API_URL` vacía o incorrecta | Debe ser la URL absoluta de la API; el `/api` se añade solo |
| La API no arranca y Flyway falla | Credenciales o host de la base mal resueltos | Revisa las cinco variables `SPRING_DATASOURCE_*` |
| El panel da `401` tras un login correcto | El proxy no reenvía `Authorization` | Ya está resuelto en `frontend/src/server.ts`; redespliega esa versión |
| El sitio tarda un minuto la primera vez | El plan free está despertando | Es normal; usa un plan pago para evitarlo |
| El sitio responde pero el catálogo sale vacío la primera vez | El sitio arrancó antes de que existiera la API | Redeploy manual del sitio para que tome `PUBLIC_API_URL` |
| La base desapareció | Postgres free caducó a los 30 días | Recupera dentro de la gracia o crea una nueva |

## Alternativa: un servidor propio con Docker Compose

Si prefieres tu VPS, el mismo producto se levanta con:

```bash
cp .env.example .env
openssl rand -base64 48        # JWT_SECRET
docker compose --profile app up -d --build --wait
```

Los puertos se publican en `127.0.0.1`, así que hace falta un proxy inverso con
TLS (Caddy o Nginx) en la misma máquina. En ese caso:

```text
PUBLIC_API_URL=http://backend:8080/api   # red interna de Compose
SITE_BASE_URL=https://tudominio.com
NG_ALLOWED_HOSTS=tudominio.com
CORS_ALLOWED_ORIGINS=https://tudominio.com
```

Y el proxy debe enviar el tráfico del sitio a `127.0.0.1:4200`.
