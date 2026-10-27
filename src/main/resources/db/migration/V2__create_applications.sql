create table applications (
                              id           bigint generated always as identity primary key,
                              user_id      bigint       not null references users (id) on delete cascade,
                              company      varchar(150) not null,
                              role         varchar(150) not null,
                              job_url      varchar(500),
                              status       varchar(20)  not null default 'APPLIED',
                              applied_date date         not null,
                              notes        text,
                              created_at   timestamptz  not null default now(),
                              last_updated timestamptz  not null default now(),
                              constraint chk_applications_status
                                  check (status in ('APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'))
);

create index idx_applications_user_status on applications (user_id, status);