-- ============================================================================
-- Xtrim — la auditoría se retira (HU-31)
--
-- La pantalla de solo lectura del historial sale del panel, y con ella la
-- maquinaria de Hibernate Envers que la alimentaba: la tabla de revisiones y
-- las tres tablas de historial. El rol AUDITOR se va con ella — no tenía
-- ninguna otra ruta (el brief: «AUDITOR accede solo a la auditoría, en modo
-- lectura»).
--
-- V1..V3 no se tocan: sus sumas de verificación ya están en las bases de datos
-- que los ejecutaron. Esta migración es la que quita el tema de auditoría.
--
-- Lo que se conserva: `activity_events`, el registro del comportamiento del
-- visitante, que alimenta el CRM y no tiene nada que ver con el historial de
-- versiones del catálogo.
-- ============================================================================

drop table if exists catalog_item_categories_history;
drop table if exists catalog_items_history;
drop table if exists discounts_history;
drop table if exists revinfo;
drop sequence if exists revinfo_seq;

delete from users where role = 'AUDITOR';

-- El rol AUDITOR se va con la auditoría: el único rol que queda es ADMIN.
alter table users drop constraint if exists ck_users_role;
alter table users add constraint ck_users_role check (role = 'ADMIN');
