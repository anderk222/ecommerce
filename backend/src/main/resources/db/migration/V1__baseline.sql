-- ============================================================================
-- Xtrim — baseline schema
--
-- Flyway owns the schema; Hibernate never creates it (tech-stack 2.3).
-- This migration defines the whole data model of the use-case document,
-- section 11: the catalog, the leads, the tracking, the orders and the
-- history tables of Hibernate Envers.
--
-- The history tables below are shaped exactly as Hibernate Envers expects
-- them, because the application runs with `ddl-auto=validate`: the schema and
-- the mappings are checked against each other on every boot.
-- ============================================================================

-- --- Staff users (HU-13) ---------------------------------------------------
create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name  varchar(255) not null,
    role          varchar(32)  not null,
    created_at    timestamptz  not null default now(),
    constraint ck_users_role check (role in ('ADMIN', 'AUDITOR'))
);

-- --- Catalog (HU-14 … HU-19) ------------------------------------------------
create table categories (
    id         bigserial primary key,
    name       varchar(255) not null,
    slug       varchar(255) not null unique,
    created_at timestamptz not null default now()
);

create table catalog_items (
    id          bigserial primary key,
    type        varchar(32)     not null,
    name        varchar(255)    not null,
    slug        varchar(255)    not null unique,
    description text            not null default '',
    price       numeric(12, 2)  not null check (price >= 0),
    -- NULL means "stock is not tracked" (mission 7.2). Only 0 means out of stock.
    stock       integer,
    images      jsonb           not null default '[]'::jsonb,
    specs       jsonb           not null default '[]'::jsonb,
    -- The name and the description, normalised: lower case and without accents,
    -- so that the public search matches "cafe" against "Café" (HU-02).
    search_text text            not null default '',
    is_featured boolean         not null default false,
    is_active   boolean         not null default true,
    deleted_at  timestamptz,
    created_at  timestamptz     not null default now(),
    updated_at  timestamptz     not null default now(),
    -- The projection of "most bought": the count of confirmed purchases.
    sales_count integer         not null default 0,
    constraint ck_catalog_items_type check (type in ('PRODUCT', 'SERVICE')),
    constraint ck_catalog_items_stock check (stock is null or stock >= 0)
);

create index ix_catalog_items_slug on catalog_items (slug);
create index ix_catalog_items_type on catalog_items (type);
create index ix_catalog_items_active on catalog_items (is_active, deleted_at);
create index ix_catalog_items_search on catalog_items (search_text);

create table catalog_item_categories (
    id          bigserial primary key,
    item_id     bigint      not null references catalog_items (id) on delete cascade,
    category_id bigint      not null references categories (id) on delete cascade,
    created_at  timestamptz not null default now()
);

create index ix_catalog_item_categories_category on catalog_item_categories (category_id);

create table discounts (
    id         bigserial primary key,
    item_id    bigint      not null references catalog_items (id) on delete cascade,
    percentage integer     not null check (percentage between 0 and 100),
    starts_at  timestamptz,
    ends_at    timestamptz,
    revoked_at timestamptz,
    created_at timestamptz  not null default now()
);

create index ix_discounts_item on discounts (item_id);

-- --- Leads (HU-11 … HU-12, HU-21) ------------------------------------------
create table leads (
    id          bigserial primary key,
    name        varchar(255) not null,
    email       varchar(255) not null unique,
    phone       varchar(64),
    province    varchar(64),
    status      varchar(32)  not null default 'NEW',
    sources     varchar(255) not null default '',
    sale_amount numeric(12, 2) not null default 0,
    outcome     varchar(32),
    consent     boolean      not null default false,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    constraint ck_leads_status check (status in ('NEW', 'IN_PROGRESS', 'CLOSED')),
    constraint ck_leads_outcome check (outcome is null or outcome in ('WON', 'LOST'))
);

create index ix_leads_status on leads (status);
create index ix_leads_province on leads (province);
create index ix_leads_created_at on leads (created_at);

create table lead_status_history (
    id              bigserial primary key,
    lead_id         bigint      not null references leads (id) on delete cascade,
    previous_status varchar(32),
    new_status      varchar(32)  not null,
    changed_by      varchar(255) not null default 'SISTEMA',
    changed_at      timestamptz  not null default now()
);

create index ix_lead_status_history_lead on lead_status_history (lead_id);

create table lead_notes (
    id         bigserial primary key,
    lead_id    bigint      not null references leads (id) on delete cascade,
    body       text        not null,
    author     varchar(255) not null default 'SISTEMA',
    created_at timestamptz  not null default now()
);

create index ix_lead_notes_lead on lead_notes (lead_id);

-- --- Anonymous tracking (HU-09, HU-10) --------------------------------------
create table visitors (
    anonymous_id uuid primary key,
    lead_id      bigint references leads (id) on delete set null,
    created_at   timestamptz not null default now(),
    last_seen_at timestamptz not null default now()
);

create index ix_visitors_lead on visitors (lead_id);

create table activity_events (
    id           bigserial primary key,
    anonymous_id uuid        not null,
    lead_id      bigint references leads (id) on delete set null,
    event_type   varchar(64) not null,
    entity_type  varchar(64),
    entity_id    bigint,
    metadata     jsonb       not null default '{}'::jsonb,
    created_at   timestamptz not null default now()
);

create index ix_activity_events_anonymous on activity_events (anonymous_id);
create index ix_activity_events_lead on activity_events (lead_id);
create index ix_activity_events_created_at on activity_events (created_at);

-- --- Orders (HU-07) ---------------------------------------------------------
create table orders (
    id           bigserial primary key,
    lead_id      bigint references leads (id) on delete set null,
    anonymous_id uuid,
    total        numeric(12, 2) not null default 0,
    item_count   integer      not null default 0,
    created_at   timestamptz  not null default now()
);

create index ix_orders_lead on orders (lead_id);
create index ix_orders_created_at on orders (created_at);

create table order_items (
    id         bigserial primary key,
    order_id   bigint         not null references orders (id) on delete cascade,
    item_id    bigint         not null references catalog_items (id),
    item_name  varchar(255)   not null,
    quantity   integer        not null check (quantity > 0),
    unit_price numeric(12, 2) not null check (unit_price >= 0),
    line_total numeric(12, 2) not null check (line_total >= 0)
);

create index ix_order_items_order on order_items (order_id);
create index ix_order_items_item on order_items (item_id);

-- --- Stock subscriptions (HU-08) --------------------------------------------
create table stock_subscriptions (
    id          bigserial primary key,
    lead_id     bigint      not null references leads (id) on delete cascade,
    item_id     bigint      not null references catalog_items (id) on delete cascade,
    notified_at timestamptz,
    created_at  timestamptz not null default now(),
    constraint uq_stock_subscription unique (lead_id, item_id)
);

create index ix_stock_subscriptions_item on stock_subscriptions (item_id);

create table notifications (
    id         bigserial primary key,
    channel    varchar(32)  not null default 'LOG',
    recipient  varchar(255) not null,
    subject    varchar(255) not null,
    body       text         not null,
    created_at timestamptz  not null default now()
);

-- ============================================================================
-- Hibernate Envers — the audit trail (HU-31)
--
-- Each history table repeats the columns of the audited table plus `rev` and
-- `revtype` (0 = insert, 1 = update, 2 = delete). Flyway creates them;
-- Hibernate only writes them.
-- ============================================================================

-- The revision number and the instant of the revision. The *type* of the
-- operation (insert, update, delete) lives in the `revtype` column of each
-- history table, not here.
create table revinfo (
    id          integer     not null primary key,
    "timestamp" bigint      not null,
    username    varchar(255) not null default 'SISTEMA'
);

-- Envers draws the number of the revision from this sequence.
create sequence revinfo_seq start with 1 increment by 50;

create table catalog_items_history (
    id           bigint        not null,
    rev          integer       not null,
    revtype      smallint      not null,
    type         varchar(32),
    name         varchar(255),
    slug         varchar(255),
    description  text,
    price        numeric(12, 2),
    stock        integer,
    images       jsonb,
    specs        jsonb,
    search_text  text,
    is_featured  boolean,
    is_active    boolean,
    deleted_at   timestamptz,
    created_at   timestamptz,
    updated_at   timestamptz,
    sales_count  integer,
    primary key (rev, id)
);

create table discounts_history (
    id         bigint      not null,
    rev        integer     not null,
    revtype    smallint    not null,
    item_id    bigint,
    percentage integer,
    starts_at  timestamptz,
    ends_at    timestamptz,
    revoked_at timestamptz,
    created_at timestamptz,
    primary key (rev, id)
);

create table catalog_item_categories_history (
    id         bigint      not null,
    rev        integer     not null,
    revtype    smallint    not null,
    created_at timestamptz,
    primary key (rev, id)
);
