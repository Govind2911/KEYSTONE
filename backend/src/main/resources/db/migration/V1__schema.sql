-- KEYSTONE schema (MySQL 8). Every change after this file must be a new versioned migration.
-- All timestamps are stored in UTC.

create table customers (
    id            bigint       not null auto_increment primary key,
    name          varchar(150) not null,
    contact_name  varchar(100),
    contact_email varchar(150),
    contact_phone varchar(30),
    address       varchar(300),
    active        boolean      not null default true,
    created_at    datetime(6)  not null default current_timestamp(6),
    constraint uq_customers_name unique (name)
);

create table sites (
    id           bigint       not null auto_increment primary key,
    customer_id  bigint       not null,
    name         varchar(150) not null,
    address_line varchar(250) not null,
    city         varchar(100) not null,
    state        varchar(100),
    postal_code  varchar(20),
    country      varchar(100),
    created_at   datetime(6)  not null default current_timestamp(6),
    constraint fk_sites_customer foreign key (customer_id) references customers (id),
    index ix_sites_customer (customer_id)
);

create table users (
    id            bigint       not null auto_increment primary key,
    name          varchar(100) not null,
    email         varchar(150) not null,
    password_hash varchar(100) not null,
    phone         varchar(30),
    role          varchar(20)  not null,
    customer_id   bigint,
    active        boolean      not null default true,
    created_at    datetime(6)  not null default current_timestamp(6),
    constraint uq_users_email unique (email),
    constraint fk_users_customer foreign key (customer_id) references customers (id),
    constraint ck_users_role check (role in ('DISPATCHER', 'TECHNICIAN', 'MANAGER', 'CUSTOMER')),
    -- a CUSTOMER user always belongs to exactly one customer organisation; staff never do
    constraint ck_users_customer check ((role = 'CUSTOMER' and customer_id is not null)
                                     or (role <> 'CUSTOMER' and customer_id is null))
);

create table parts (
    id          bigint         not null auto_increment primary key,
    sku         varchar(50)    not null,
    name        varchar(150)   not null,
    description varchar(500),
    unit_cost   decimal(12, 2) not null,
    stock_qty   int            not null,
    created_at  datetime(6)    not null default current_timestamp(6),
    constraint uq_parts_sku unique (sku),
    constraint ck_parts_cost check (unit_cost >= 0),
    -- last line of defence: stock can never go negative
    constraint ck_parts_stock_not_negative check (stock_qty >= 0)
);

create table work_orders (
    id                bigint        not null auto_increment primary key,
    code              varchar(30)   not null,
    title             varchar(150)  not null,
    description       varchar(3000) not null,
    status            varchar(20)   not null,
    priority          varchar(20)   not null,
    customer_id       bigint        not null,
    site_id           bigint        not null,
    assigned_to_id    bigint,
    created_by_id     bigint        not null,
    scheduled_at      datetime(6),
    started_at        datetime(6),
    completed_at      datetime(6),
    closed_at         datetime(6),
    sla_due_at        datetime(6)   not null,
    sla_breached      boolean       not null default false,
    sla_breached_at   datetime(6),
    sla_risk_notified boolean       not null default false,
    created_at        datetime(6)   not null default current_timestamp(6),
    updated_at        datetime(6)   not null default current_timestamp(6),
    version           bigint        not null default 0,
    constraint uq_work_orders_code unique (code),
    constraint fk_wo_customer foreign key (customer_id) references customers (id),
    constraint fk_wo_site foreign key (site_id) references sites (id),
    constraint fk_wo_assignee foreign key (assigned_to_id) references users (id),
    constraint fk_wo_creator foreign key (created_by_id) references users (id),
    constraint ck_wo_status check (status in ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED')),
    constraint ck_wo_priority check (priority in ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    index ix_wo_status (status),
    index ix_wo_customer (customer_id),
    index ix_wo_site (site_id),
    index ix_wo_assignee (assigned_to_id),
    index ix_wo_sla_due (sla_due_at),
    index ix_wo_created (created_at)
);

create table work_order_status_history (
    id            bigint      not null auto_increment primary key,
    work_order_id bigint      not null,
    from_status   varchar(20),
    to_status     varchar(20) not null,
    changed_by_id bigint      not null,
    changed_at    datetime(6) not null default current_timestamp(6),
    note          varchar(500),
    constraint fk_hist_wo foreign key (work_order_id) references work_orders (id),
    constraint fk_hist_user foreign key (changed_by_id) references users (id),
    index ix_history_wo (work_order_id, changed_at)
);

create table part_usages (
    id            bigint         not null auto_increment primary key,
    work_order_id bigint         not null,
    part_id       bigint         not null,
    qty_used      int            not null,
    unit_cost     decimal(12, 2) not null,   -- price snapshot at time of use
    used_by_id    bigint         not null,
    used_at       datetime(6)    not null default current_timestamp(6),
    note          varchar(500),
    constraint fk_usage_wo foreign key (work_order_id) references work_orders (id),
    constraint fk_usage_part foreign key (part_id) references parts (id),
    constraint fk_usage_user foreign key (used_by_id) references users (id),
    constraint ck_usage_qty check (qty_used > 0),
    index ix_usage_wo (work_order_id)
);

create table time_logs (
    id            bigint      not null auto_increment primary key,
    work_order_id bigint      not null,
    technician_id bigint      not null,
    minutes       int         not null,
    note          varchar(500),
    logged_at     datetime(6) not null default current_timestamp(6),
    constraint fk_time_wo foreign key (work_order_id) references work_orders (id),
    constraint fk_time_user foreign key (technician_id) references users (id),
    constraint ck_time_minutes check (minutes between 1 and 1440),
    index ix_time_wo (work_order_id)
);

create table attachments (
    id             bigint       not null auto_increment primary key,
    work_order_id  bigint       not null,
    file_name      varchar(200) not null,
    content_type   varchar(100) not null,
    size_bytes     bigint       not null,
    storage_key    varchar(300) not null,
    uploaded_by_id bigint       not null,
    uploaded_at    datetime(6)  not null default current_timestamp(6),
    constraint fk_attach_wo foreign key (work_order_id) references work_orders (id),
    constraint fk_attach_user foreign key (uploaded_by_id) references users (id),
    index ix_attach_wo (work_order_id)
);

create table notifications (
    id            bigint       not null auto_increment primary key,
    user_id       bigint       not null,
    type          varchar(30)  not null,
    message       varchar(300) not null,
    work_order_id bigint,
    seen          boolean      not null default false,
    created_at    datetime(6)  not null default current_timestamp(6),
    constraint fk_notif_user foreign key (user_id) references users (id),
    index ix_notif_user (user_id, seen, created_at)
);
