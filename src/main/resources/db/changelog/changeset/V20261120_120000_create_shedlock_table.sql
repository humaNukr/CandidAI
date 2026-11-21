-- liquibase formatted sql

-- changeset artem:V20261120_120000_create_shedlock_table
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

