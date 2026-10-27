create table status_history (
                                id             bigint generated always as identity primary key,
                                application_id bigint      not null references applications (id) on delete cascade,
                                old_status     varchar(20),
                                new_status     varchar(20) not null,
                                changed_at     timestamptz not null default now()
);

create index idx_status_history_application on status_history (application_id);