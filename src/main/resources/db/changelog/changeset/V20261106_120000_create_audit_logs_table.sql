-- liquibase formatted sql

-- changeset artem:V20261106_120000_create_audit_logs_table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    method_name VARCHAR(128) NOT NULL,
    user_id UUID,
    user_email VARCHAR(255),
    user_role VARCHAR(64),
    trace_id VARCHAR(64),
    client_ip VARCHAR(64),
    parameters TEXT,
    execution_time_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    exception_class VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_success ON audit_logs(success);

CREATE OR REPLACE VIEW audit_log AS
SELECT * FROM audit_logs;

