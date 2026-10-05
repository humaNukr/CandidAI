-- liquibase formatted sql

-- changeset dev3:V20261005_100000_seed_e2e_data
INSERT INTO companies (id, name, created_at)
VALUES ('c0000000-0000-0000-0000-000000000003', 'CandidAI Test Company', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, role, created_at)
VALUES ('a0000000-0000-0000-0000-000000000001', 'Test Author', 'author@candidai.ua', 'RECRUITER', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, full_name, email, role, created_at)
VALUES ('c0000000-0000-0000-0000-000000000001', 'Test Candidate', 'candidate@candidai.ua', 'CANDIDATE', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
