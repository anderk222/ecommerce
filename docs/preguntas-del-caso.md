# Preguntas del caso práctico y escalabilidad

> Documento de apoyo del [README](../README.md): las respuestas completas a las tres preguntas del caso práctico y el plan de escalabilidad.

---

## Las respuestas del caso práctico

### 1. El tráfico orgánico cayó un 40%

**Qué revisar primero, en este orden:**

1. **El estado del sitio y su disponibilidad**, no el tráfico: si el sitio devuelve 5xx o tarda, el
   buscador lo interpreta como un problema y baja la clasificación antes de que caiga el tráfico.
   La primera pregunta es si el tráfico *cayó* o si *cayó la posición*.
2. **Los registros del servidor y del CDN en la ventana exacta de la caída**, en busca de 5xx, de
   timeouts y de un aumento de la latencia (TTFB).
3. **Google Search Console**: el informe *Cobertura*, el de *Rendimiento* y el de *Manual de acciones
   de Google*. Una caída del 40% en las páginas de categoría suele ser una de dos cosas: una
   desindexación (el *sitemap* cayó o la ruta cambió) o un contenido bloqueado por una
   cabecera.
4. **El estado del despliegue**: una versión nueva con un error 500 en el SSR deja la mitad de las
   páginas vacías para el buscador, aunque el sitio "funcione" para una persona.

**Cómo diagnosticarlo:**

- Comparar las páginas que **cayeron** con las que **no**: si solo cayeron las de listado, el
  problema está en el SSR de los listados (y en el *sitemap*, que las lista todas).
- Buscar en los registros un `robots.txt` o un `noindex` nuevo: un cambio en la plantilla del
  `<head>` se lleva por delante la indexación entera.
- Medir el **CLS y el LCP** de las páginas afectadas. Una caída del 40% con las posiciones
  intactas y la impressions de Search Console también intactas apunta al rastreo: las páginas ya no
  se recorren.
- Comprobar que el `sitemap.xml` sigue respondiendo 200 y que devuelve las URLs esperadas. Es la
  comprobación más barata y descarta la causa más frecuente.

**Soluciones posibles:**

- **Devolver el `sitemap.xml` y el `robots.txt` desde el edge**, no desde la aplicación: si la
  aplicación se cae, el buscador recibe un 503 y la pierde entera.
- **Un plano de mantenimiento**: si hay que desplegar una versión rota, que sirva un 503 con
  `Retry-After` en lugar de un 500 con HTML vacío.
- **Sembrar el *canonical* y el JSON-LD en el HTML del servidor** —esto ya está hecho— para que el
  rastreo no dependa de ejecutar JavaScript.
- **Vigilar la disponibilidad y el rastreo** con una alerta sobre el código de estado y sobre el
  número de URLs del *sitemap*.
- Si la caída viene de un cambio de contenido (un EMP de Google, un cambio de plantilla), el
  diagnóstico es el inverso: Search Console muestra las páginas con el CTR a la baja y sin cambio
  en las impresiones, y la respuesta es revisar los títulos y las descripciones, no tocar la
  infraestructura.

### 2. El formulario falla de forma intermitente

**Qué revisar primero:**

1. **Si falla para todos o para algunos.** Un fallo del 100% es un despliegue; uno del 5% es una
   condición de carrera o un recurso saturado.
2. **Los registros del servidor**, filtrados por la ruta del formulario, alrededor de las horas en
   que falla: errores 4xx (validación, un correo que no llega) y 5xx (el servidor).
3. **La ruta de salida**: si el correo *se envía* pero no *llega*, el formulario funciona y el
   problema es el proveedor de correo o el filtrado de spam.

**Cómo diagnosticarlo:**

- **Reproducir con la misma forma**: un formulario intermitente casi siempre es una de estas cuatro:
  - **Una carrera en el servidor**: dos peticiones simultáneas sobre el mismo registro. La forma
    canónica es el `upsert` por correo de los leads: dos personas con el mismo correo a la vez. La
    columna `email` es `unique` y el segundo insert falla. **El diagnóstico**: en los registros,
    `UniqueConstraintViolationException` o `DuplicateKeyException` en la tabla `leads`.
  - **Un recurso saturado**: un pool de conexiones agotado, un pool de hilos lleno o un *timeout* de
    la base de datos cuando la tabla `leads` crece. **El diagnóstico**: el tiempo de respuesta de
    la ruta contra el de la misma consulta en la consola, y el estado del pool en las métricas.
  - **Un cliente que se va antes de tiempo**: el visitante cierra la pestaña mientras la petición
    sigue en vuelo. Parece un fallo del servidor y es del navegador.
  - **Una dependencia externa**: el correo, el captcha o un servicio de terceros que responde
    intermitentemente.
- **Comparar los identificadores de las peticiones**: si las que fallan comparten un mismo
  identificador de visitante, es un problema del cliente; si son distintos, es del servidor.

**Soluciones posibles:**

- **Hacer la captura idempotente de verdad**: el `upsert` por correo con `INSERT … ON CONFLICT
  DO UPDATE`, en vez de un `select` seguido de un `insert`. La operación es atómica y el conflicto
  desaparece.
- **Aislar la captura del resto de la petición**: el evento y la notificación del stock se
  escriben en su propia transacción, después de que el lead está guardado. Un fallo del tracking
  no puede tumbar el formulario.
- **Un reintento con espera exponencial** en las dependencias externas, y una cola de reintentos para
  el correo.
- **Devolver 202 y enviar el correo aparte**, si el correo no es esencial para la respuesta: el
  visitante no espera al proveedor de correo.
- **Registrar el motivo del fallo** con un identificador de correlación que el visitante pueda
  reportar: sin eso, "intermitente" no se puede diagnosticar.

### 3. El sitio va lento en los móviles

> **Esto ya está hecho y medido.** El sitio se midió por primera vez, se corrigió y quedaron los
> números: Lighthouse en modo móvil pasó de **32 a 94** en la home y el CLS de **1,645 a 0**. Lo
> que sigue es el razonamiento, con lo que se encontró y lo que se hizo.

**Qué revisar primero:**

1. **El peso de la primera carga**, y no el de la página: en el móvil manda el LCP y el total
   bloqueante. La hoja de estilos de Fomantic eran 1.64 MB sin comprimir y ~486 KB de
   JavaScript de terceros. Era el mayor bloqueador del proyecto, y había que medirlo, no
   suponerlo.
2. **El LCP y el CLS** de la página real, en un móvil de gama media y con la red 4G limitada: el
   Lighthouse de escritorio miente.
3. **El peso de las imágenes**: las del catálogo son las que más pesan, y son las que se ven primero.

**Cómo diagnosticarlo:**

- **Lighthouse en modo móvil**, con la CPU y la red limitadas, sobre la página más pesada (el
  listado, no la home). El script que lo hace está en el repositorio:
  [`scripts/lighthouse.sh`](../scripts/lighthouse.sh).
- **La pestaña *Network* del móvil real**: qué se descarga, en qué orden y cuánto bloquea cada
  script. Un `main.js` de 320 KB y un `scripts.js` de 492 KB (jQuery + Fomantic) bloquean el
  hilo principal antes de que aparezca nada.
- **El Core Web Vitals** de las URLs reales, con datos de campo (CrUX) y no solo de laboratorio:
  el laboratorio mide el 10% mejor de los visitantes reales.
- **La caché**: si los recursos estáticos se sirven sin `Cache-Control: immutable`, cada visita
  los vuelve a pedir.
- **El *CLS*, con la fuente y sin ella**: si el texto se mueve cuando llega la fuente web, el
  culpable es la fuente, no el contenido. Bloquear las fuentes en el panel de red lo demuestra en
  un minuto.

**Soluciones posibles — y las que se aplicaron:**

- ~~**Cargar Fomantic y jQuery de forma diferida**, o quitarlos de la ruta crítica~~ → **hecho**:
  el único comportamiento de Fomantic que usaba el sitio era el desplegable de «Ordenar», y hoy
  es un `<select>` nativo. **jQuery y el JS de Fomantic no se descargan** (489 KB menos).
- ~~**Autohospedar y recortar las fuentes**~~ → **hecho**: el subconjunto latino de Lato son 33 KB
  por peso en lugar de 174 KB, con `preload` de los dos pesos que el texto usa. Y además el
  *fallback* tiene **las métricas de Lato** (`size-adjust`, `ascent-override`…): sin eso, el texto
  se pinta con Arial y se mueve cuando llega Lato.
- ~~**Declarar el tamaño de las imágenes** (`width`/`height`) y usar `loading="lazy"`~~ →
  **hecho**: las imágenes son `<img>` de verdad, con sus dimensiones, `loading="lazy"` y
  `decoding="async"`. La primera diapositiva del carrusel es la que tiene la prioridad.
- **Servir las imágenes en formatos modernos** (AVIF o WebP) y con `srcset` para que el móvil
  descargue la versión pequeña. *Pendiente: las imágenes son URLs de un host externo, y el
  catálogo no guarda ficheros.*
- **Dividir el JavaScript por ruta**: las páginas del panel y el tablero no tienen que cargar el
  bundle de la home.
- **Un CDN con `Cache-Control: immutable`** para los recursos con hash en el nombre, y
  `stale-while-revalidate` para el HTML.
- ~~**Reducir el CSS**~~ → **hecho**: una poda en el build deja las 1 749 reglas que el sitio puede
  necesitar (de 13 752) y el CSS viaja **dentro del documento**, no en una segunda petición. El
  servidor comprime la respuesta.
- **Que el servidor y el navegador pidan lo mismo**: si el navegador vuelve a preguntar por lo que
  el servidor ya envió, la página se monta dos veces y el esqueleto de carga mueve todo. Con el
  mapa de orígenes de Angular, el dato viaja en el HTML.

---

## La respuesta de escalabilidad

**El objetivo: de 100 a 10.000 leads al día.** Son 0,1 leads por segundo en el caso de hoy y 6
leads por segundo en el más alto: **60 veces más**. Es un salto que no se resuelve agrandando la
base de datos, sino separando lo que escribe de lo que lee.

### Dónde está el cuello de botella hoy

| Punto | Qué lo sostiene hoy | Qué pasa a 10.000/día |
| --- | --- | --- |
| **El registro de la actividad** | Una escritura por evento de visitante | El mayor volumen: miles de escrituras por minuto |
| **El correo y el evento** | Se escriben dentro de la transacción | Una transacción larga que bloquea filas |
| **La búsqueda del catálogo** | Una consulta con `LIKE` sobre `search_text` | Sigue igual: el catálogo es pequeño y cabe en caché |
| **El CRM** | Pocas consultas al día | Sigue igual: es un panel para personas |

**La observación clave: el catálogo no crece. Crece la actividad.** Diez mil visitas al día no
son diez mil ítems: son diez mil filas en `activity_events` y quizá mil leads. El catálogo —la
tabla que más se lee— es la más pequeña.

### El plan

**Medir primero (sin cambiar nada).** Las métricas que mandan: eventos por segundo, latencia del
`POST /api/public/events`, duración de la transacción del checkout, tamaño de la tabla de eventos y
la tasa de errores. Con los números, el resto es una cuenta.

**Quitar la carga del camino crítico.**

- El evento de tracking se escribe **fuera** de la transacción que lo origina, con una cola
  (`REQUIRES_NEW` ya está; en producción, una cola de verdad).
- El correo y la notificación del stock se escriben **después**, en su propia transacción, con un
  reintento. Ya es la estructura; falta el transporte.

**Partir el dato.** La tabla `activity_events` es de solo inserciones y crece sin límite:
es una tabla de series de tiempo, no relacional. El camino es:

- **Una partición por mes** (PostgreSQL lo hace de forma nativa con `PARTITION BY RANGE`).
- **Un archivo por día en un almacén de objetos** (S3, o el equivalente), y la base solo conserva
  la ventana caliente —los últimos 90 días, que es lo que la consulta del CRM mira.
- Los agregados —los indicadores, el "más comprado"— se calculan en una vista materializada o en
  una tabla que se refresca por lotes.

**Los lectores.** El catálogo es de solo lectura y cambia poco:

- **Redis como caché** (ya está, con cinco minutos de vida y vaciado en cada escritura).
- Si el catálogo creciera de verdad, **una réplica de lectura** y el enrutado de las consultas de
  lectura hacia ella.

**La escritura de los leads.** Diez mil capturas al día son ~0,12 por segundo: PostgreSQL
de sobra. Lo que hay que fortalecer:

- El **upsert por correo** atómico (`ON CONFLICT`), para que dos personas con el mismo correo no
  se pisen (es, además, la causa más probable del formulario intermitente).
- El **índice único** sobre `leads.email` —ya existe— y el `INSERT ... ON CONFLICT DO UPDATE` que
  hace idempotente la captura.
- La **conexión**: el pool se dimensiona contra el número de instancias, no al revés.

**El despliegue.** El API se escala horizontalmente: es sin estado (la sesión es el
JWT, no el servidor) y el pool de conexiones se reparte entre las instancias.

| Pieza | Cómo escala |
| --- | --- |
| La API | Horizontal: instancias sin estado detrás de un balanceador |
| PostgreSQL | Vertical primero (más memoria y CPU); réplicas de lectura cuando el catálogo lo justifique |
| Redis | Vertical, y un *cluster* cuando el caché pesa |
| Los eventos | Cola → trabajadores; a partir de ahí, escala horizontalmente |
| El correo | A una cola con reintentos, con varios trabajadores |

### Lo que **no** haría

- **No tocaría la base de datos de entrada.** Diez mil escrituras al día son una carga trivial para
  PostgreSQL. El problema nunca es la base: es el número de **transacciones largas** que la
  atraviesan.
- **No separaría el CRM del catálogo.** Son volúmenes distintos: uno crece, el otro no.
- **No usaría microservicios.** Un monolito modular con fronteras claras (que es lo que es este
  proyecto) escala hasta mucho más lejos de 10.000 leads al día, y cuesta una fracción.

### El resumen

> El catálogo es pequeño y se cachea. La actividad es grande y se archiva. Los leads son
> moderados y se indexan. **El trabajo no es agrandar la base de datos: es sacar del camino
> crítico lo que no necesita estar ahí.**
