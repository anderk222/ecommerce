-- ============================================================================
-- Xtrim — seed catalog
--
-- The example data of HU-32: categories, products, services and a couple of
-- discounts in force. It is inserted by Flyway so that a fresh database is
-- immediately demonstrable, and so that the development stack never depends
-- on a manual step.
--
-- The identifiers are explicit: the seed is idempotent by construction
-- (the migration runs once) and readable in the database.
-- ============================================================================

-- --- Categories -------------------------------------------------------------
insert into categories (id, name, slug, created_at) values
  (1, 'Tecnología',   'tecnologia',   now()),
  (2, 'Hogar',        'hogar',        now()),
  (3, 'Ropa',         'ropa',         now()),
  (4, 'Oficina',      'oficina',      now()),
  (5, 'Servicios',    'servicios',    now());

select setval('categories_id_seq', (select max(id) from categories));

-- --- Items ------------------------------------------------------------------
insert into catalog_items
  (id, type, name, slug, description, price, stock, images, specs, search_text,
   is_featured, is_active, created_at, updated_at, sales_count)
values
  (1, 'PRODUCT', 'Router WiFi 6 AX3000', 'router-wifi-6-ax3000',
   'Router de doble banda con WiFi 6, ideal para Homes y oficinas con varios dispositivos conectados al mismo tiempo.',
   89.90, 12,
   '["https://placehold.co/800x600?text=Router+WiFi+6"]',
   '[{"name":"Banda","value":"2.4 GHz + 5 GHz"},{"name":"Velocidad","value":"AX3000 Mbps"},{"name":"Puertos","value":"4 LAN + 1 WAN"}]',
   'router wifi 6 ax3000 doble banda velocidad mbps puertos lan wan', true, true, now(), now(), 14),

  (2, 'PRODUCT', 'Laptop Nova 14 pulgadas', 'laptop-nova-14-pulgadas',
   'Laptop de 14 pulgadas con 8 GB de memoria, 256 GB de disco SSD y pantalla Full HD, para el trabajo y el estudio.',
   549.00, 5,
   '["https://placehold.co/800x600?text=Laptop+Nova+14"]',
   '[{"name":"Pantalla","value":"14 pulgadas Full HD"},{"name":"Memoria","value":"8 GB RAM"},{"name":"Disco","value":"256 GB SSD"}]',
   'laptop nova 14 pulgadas memoria ram disco ssd pantalla hd', true, true, now(), now(), 9),

  (3, 'PRODUCT', 'Auriculares Bluetooth SoundBeat', 'auriculares-bluetooth-soundbeat',
   'Auriculares inalámbricos con cancelación de ruido, 30 horas de batería y estuche de carga.',
   59.50, 25,
   '["https://placehold.co/800x600?text=Auriculares+SoundBeat"]',
   '[{"name":"Batería","value":"30 horas"},{"name":"Conexión","value":"Bluetooth 5.3"},{"name":"Color","value":"Negro"}]',
   'auriculares bluetooth soundbeat cancelacion de ruido bateria horas', false, true, now(), now(), 21),

  (4, 'PRODUCT', 'Smart TV 50 pulgadas 4K', 'smart-tv-50-pulgadas-4k',
   'Televisor inteligente de 50 pulgadas con resolución 4K, aplicaciones integradas y control por voz.',
   429.00, 3,
   '["https://placehold.co/800x600?text=Smart+TV+50+4K"]',
   '[{"name":"Pantalla","value":"50 pulgadas 4K"},{"name":"Smart TV","value":"Sí"},{"name":"HDMI","value":"3 puertos"}]',
   'smart tv 50 pulgadas 4k television inteligente aplicaciones', false, true, now(), now(), 6),

  (5, 'PRODUCT', 'Impresora Láser Color', 'impresora-laser-color',
   'Impresora láser a color de 20 páginas por minuto, ideal para oficinas pequeñas y дома.',
   219.00, 8,
   '["https://placehold.co/800x600?text=Impresora+Laser+Color"]',
   '[{"name":"Velocidad","value":"20 ppm"},{"name":"Color","value":"CMYK"},{"name":"Conexión","value":"WiFi y USB"}]',
   'impresora laser color ppm cmyk conexion wifi usb', false, true, now(), now(), 4),

  (6, 'PRODUCT', 'Cámara de seguridad IP 1080p', 'camara-seguridad-ip-1080p',
   'Cámara de videovigilancia IP con visión nocturna, detección de movimiento y grabación en la nube.',
   39.90, 30,
   '["https://placehold.co/800x600?text=Camara+Seguridad+IP"]',
   '[{"name":"Resolución","value":"1080p"},{"name":"Visión nocturna","value":"Sí"},{"name":"Alimentación","value":"USB"}]',
   'camara seguridad ip 1080p vision nocturna deteccion movimiento', false, true, now(), now(), 17),

  (7, 'PRODUCT', 'Licuadora PowerMix 1200W', 'licuadora-powermix-1200w',
   'Licuadora de vaso de vidrio de 1200 W con velocidad variable y función para picar hielo.',
   39.50, 0,
   '["https://placehold.co/800x600?text=Licuadora+PowerMix"]',
   '[{"name":"Potencia","value":"1200 W"},{"name":"Vaso","value":"Vidrio 1.5 L"},{"name":"Velocidades","value":"3"}]',
   'licuadora powermix 1200w vaso vidrio velocidad hielo', false, true, now(), now(), 11),

  (8, 'PRODUCT', 'Juego de sábanas de algodón', 'juego-sabanas-algodon',
   'Juego de sábanas de algodón peinado de cuatro piezas, suave y resistente al lavado.',
   24.90, 40,
   '["https://placehold.co/800x600?text=Juego+Sabanas+Algodon"]',
   '[{"name":"Material","value":"100% algodón"},{"name":"Piezas","value":"4"},{"name":"Tallas","value":"Matrimonio y doble"}]',
   'juego sabanas algodon piezasido suave lavado', false, true, now(), now(), 2),

  (9, 'PRODUCT', 'Silla de oficina ergonómica', 'sila-oficina-ergonomica',
   'Silla de oficina con respaldo lumbar, altura regulable y reposabrazos 3D.',
   129.00, 7,
   '["https://placehold.co/800x600?text=Silla+Oficina+Ergonomica"]',
   '[{"name":"Respaldo","value":"Lumbar"},{"name":"Altura","value":"Regulable"},{"name":"Material","value":"Tela mesh"}]',
   'sila de oficina ergonomica respaldo lumbar altura regulable reposabrazos', false, true, now(), now(), 5),

  (10, 'PRODUCT', 'Tablet 10 pulgadas 64 GB', 'tablet-10-pulgadas-64-gb',
   'Tableta de 10 pulgadas con 64 GB de almacenamiento, pantalla IPS y soporte para teclado.',
   189.00, 2,
   '["https://placehold.co/800x600?text=Tablet+10+Pulgadas"]',
   '[{"name":"Pantalla","value":"10 pulgadas IPS"},{"name":"Almacenamiento","value":"64 GB"},{"name":"Conectividad","value":"WiFi + LTE"}]',
   'tablet 10 pulgadas 64gb almacenamiento pantalla ips teclado', false, true, now(), now(), 8),

  (11, 'SERVICE', 'Instalación de internet en casa', 'instalacion-internet-casa',
   'Instalación de internet en casa: visita técnica, cableado, configuración del router y pruebas de velocidad. Incluye el primer mes de servicio.',
   45.00, null,
   '["https://placehold.co/800x600?text=Instalacion+Internet+Casa"]',
   '[{"name":"Cobertura","value":"Quito y Sangolquí"},{"name":"Incluye","value":"Router y configuración"},{"name":"Duración","value":"2 horas"}]',
   'instalacion internet casa visita tecnica cableado router configuracion', true, true, now(), now(), 31),

  (12, 'SERVICE', 'Soporte técnico mensual', 'soporte-tecnico-mensual',
   'Mantenimiento preventivo de computers, limpieza, actualización de software y soporte remoto ilimitado durante el mes.',
   29.90, null,
   '["https://placehold.co/800x600?text=Soporte+Tecnico+Mensual"]',
   '[{"name":"Visitas","value":"2 al mes"},{"name":"Modalidad","value":"Presencial o remota"},{"name":"Respuesta","value":"24 horas"}]',
   'soporte tecnico mensual mantenimiento computers software remoto', false, true, now(), now(), 19),

  (13, 'SERVICE', 'Desarrollo de tienda en línea', 'desarrollo-tienda-en-linea',
   'Diseño y desarrollo de una tienda en línea a medida, con catálogo, carrito, checkout y panel de administración.',
   890.00, null,
   '["https://placehold.co/800x600?text=Desarrollo+Tienda+Online"]',
   '[{"name":"Plazo","value":"4 a 6 semanas"},{"name":"Incluye","value":"Catálogo y pagos"},{"name":"Entrega","value":"Código fuente"}]',
   'desarrollo tienda en linea a medida catalogo carrito checkout panel', true, true, now(), now(), 3),

  (14, 'SERVICE', 'Auditoría de seguridad informática', 'auditoria-seguridad-informatica',
   'Revisión de equipos y redes: análisis de vulnerabilidades, recomendaciones y un informe de plan de acción.',
   320.00, null,
   '["https://placehold.co/800x600?text=Auditoria+Seguridad+Informatica"]',
   '[{"name":"Alcance","value":"Hasta 10 equipos"},{"name":"Entregable","value":"Informe PDF"},{"name":"Duración","value":"5 días"}]',
   'auditoria seguridad informatica vulnerabilidades equipos redes informe', false, true, now(), now(), 1);

select setval('catalog_items_id_seq', (select max(id) from catalog_items));

-- --- Categories of the items ------------------------------------------------
insert into catalog_item_categories (item_id, category_id, created_at) values
  (1, 1, now()), (2, 1, now()), (3, 1, now()), (4, 1, now()),
  (5, 4, now()), (6, 1, now()), (10, 1, now()),
  (7, 2, now()), (8, 3, now()), (9, 4, now()),
  (11, 5, now()), (12, 5, now()), (13, 5, now()), (14, 5, now());

-- --- Discounts in force -----------------------------------------------------
-- One current discount per item (mission 7.7). The prices are calculated by
-- the server at read time, from these rows.
insert into discounts (id, item_id, percentage, starts_at, ends_at, revoked_at, created_at) values
  (1, 3,  20, now() - interval '10 days', null,                    null, now()),
  (2, 6,  15, now() - interval '5 days',  now() + interval '25 days', null, now()),
  (3, 1,  10, now() - interval '2 days',  null,                    null, now()),
  (4, 12, 25, now() - interval '1 day',   null,                    null, now()),
  -- A discount that already ended: it must not apply.
  (5, 2,  30, now() - interval '60 days', now() - interval '30 days', null, now());

select setval('discounts_id_seq', (select max(id) from discounts));
