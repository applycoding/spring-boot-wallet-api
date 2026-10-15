create table app_users (
    id uuid primary key,
    email varchar(255) not null,
    password_hash varchar(100) not null,
    created_at timestamp(6) with time zone not null,
    constraint uq_app_users_email unique (email)
);

create table accounts (
    id uuid primary key,
    owner_id uuid,
    account_type varchar(16) not null,
    name varchar(80) not null,
    currency varchar(3) not null,
    balance numeric(19, 4) not null,
    version bigint not null,
    created_at timestamp(6) with time zone not null,
    constraint fk_accounts_owner foreign key (owner_id) references app_users (id),
    constraint ck_accounts_type check (account_type in ('CUSTOMER', 'CLEARING')),
    constraint ck_accounts_owner check (
        (account_type = 'CUSTOMER' and owner_id is not null)
        or (account_type = 'CLEARING' and owner_id is null)
    )
);

create index ix_accounts_owner on accounts (owner_id);

create table clearing_accounts (
    currency varchar(3) primary key,
    account_id uuid not null,
    constraint uq_clearing_account unique (account_id),
    constraint fk_clearing_account foreign key (account_id) references accounts (id)
);

create sequence journal_sequence start with 1 increment by 1;

create table journals (
    id uuid primary key,
    journal_type varchar(16) not null,
    created_by uuid not null,
    created_at timestamp(6) with time zone not null,
    constraint ck_journals_type check (journal_type in ('DEPOSIT', 'TRANSFER')),
    constraint fk_journals_created_by foreign key (created_by) references app_users (id)
);

create table ledger_entries (
    id uuid primary key,
    journal_id uuid not null,
    account_id uuid not null,
    direction varchar(8) not null,
    amount numeric(19, 4) not null,
    currency varchar(3) not null,
    balance_after numeric(19, 4) not null,
    posting_sequence bigint not null,
    created_at timestamp(6) with time zone not null,
    constraint ck_entries_direction check (direction in ('DEBIT', 'CREDIT')),
    constraint ck_entries_amount_positive check (amount > 0),
    constraint fk_entries_journal foreign key (journal_id) references journals (id),
    constraint fk_entries_account foreign key (account_id) references accounts (id)
);

create index ix_entries_account_sequence on ledger_entries (account_id, posting_sequence);
create index ix_entries_journal on ledger_entries (journal_id);

create table idempotency_records (
    id uuid primary key,
    user_id uuid not null,
    idempotency_key varchar(200) not null,
    operation varchar(32) not null,
    request_hash varchar(64) not null,
    response_body varchar(8000) not null,
    created_at timestamp(6) with time zone not null,
    constraint uq_idempotency_user_key unique (user_id, idempotency_key),
    constraint fk_idempotency_user foreign key (user_id) references app_users (id)
);
