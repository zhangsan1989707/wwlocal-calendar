create table if not exists departments (
    id varchar(64) primary key,
    name varchar(120) not null,
    parent_id varchar(64),
    sort_order integer default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists users (
    id varchar(64) primary key,
    name varchar(80) not null,
    email varchar(160),
    mobile varchar(40),
    department_id varchar(64) references departments(id) on delete set null,
    status varchar(40) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists calendars (
    id varchar(64) primary key,
    name varchar(120) not null,
    owner_user_id varchar(64) references users(id) on delete set null,
    type varchar(40) not null,
    color varchar(32),
    visible boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists organization_calendars (
    id varchar(64) primary key,
    name varchar(120) not null,
    scope_type varchar(40) not null,
    color varchar(32),
    created_by varchar(64) references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists schedules (
    id varchar(64) primary key,
    title varchar(180) not null,
    calendar_id varchar(64) not null references calendars(id) on delete cascade,
    start_at timestamptz not null,
    end_at timestamptz not null,
    location varchar(180),
    description text,
    created_by varchar(64) references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists attachments (
    id varchar(64) primary key,
    schedule_id varchar(64) not null references schedules(id) on delete cascade,
    file_name varchar(180) not null,
    file_url varchar(500) not null,
    file_size bigint default 0,
    uploaded_by varchar(64) references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists export_tasks (
    id varchar(64) primary key,
    task_type varchar(80) not null,
    status varchar(40) not null,
    file_url varchar(500),
    requested_by varchar(64) references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists audit_logs (
    id varchar(64) primary key,
    actor_user_id varchar(64) references users(id) on delete set null,
    action varchar(120) not null,
    target_type varchar(80) not null,
    target_id varchar(64),
    detail text,
    created_at timestamptz not null default now()
);

create table if not exists backup_restores (
    id varchar(64) primary key,
    operation_type varchar(40) not null,
    status varchar(40) not null,
    file_url varchar(500),
    created_by varchar(64) references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists system_configs (
    id varchar(64) primary key,
    config_key varchar(120) not null unique,
    config_value text not null,
    description varchar(240),
    updated_by varchar(64) references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
