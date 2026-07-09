create table rag_repositories (
    id uuid primary key,
    project_id varchar(255) not null unique,
    project_path varchar(500) not null,
    default_branch varchar(255) not null,
    docs_path_regex varchar(1000) not null,
    rag_collection varchar(255) not null,
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table rag_sync_state (
    id uuid primary key,
    repository_id uuid not null references rag_repositories(id),
    branch varchar(255) not null,
    last_successful_commit_sha varchar(255),
    last_success_at timestamp with time zone,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_rag_sync_state_repository_branch unique (repository_id, branch)
);

create table rag_sync_jobs (
    id uuid primary key,
    repository_id uuid not null references rag_repositories(id),
    job_type varchar(32) not null,
    branch varchar(255) not null,
    before_sha varchar(255),
    after_sha varchar(255) not null,
    status varchar(32) not null,
    attempts integer not null,
    error_message varchar(4000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    constraint uq_rag_sync_job_repository_branch_sha unique (repository_id, branch, after_sha)
);

create index idx_rag_sync_jobs_status_created_at on rag_sync_jobs(status, created_at);

create table rag_documents (
    id uuid primary key,
    repository_id uuid not null references rag_repositories(id),
    branch varchar(255) not null,
    path varchar(2000) not null,
    doc_id varchar(1000) not null,
    content_hash varchar(64) not null,
    commit_sha varchar(255) not null,
    rag_document_id varchar(1000) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_rag_documents_repository_branch_doc_id unique (repository_id, branch, doc_id),
    constraint uq_rag_documents_repository_branch_path unique (repository_id, branch, path)
);
