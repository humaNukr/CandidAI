-- liquibase formatted sql

-- changeset anastasia:V20260928_170000_create_interviews_table
CREATE TABLE IF NOT EXISTS interviews (
                                          id UUID PRIMARY KEY,
                                          application_id UUID NOT NULL,
                                          interviewer_id UUID,
                                          interviewer_name VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               duration_minutes INT DEFAULT 60 NOT NULL,
                               meeting_link VARCHAR(500),
    notes TEXT,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_interviewer FOREIGN KEY (interviewer_id) REFERENCES users(id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_interviews_application_id ON interviews(application_id);
CREATE INDEX IF NOT EXISTS idx_interviews_scheduled_at ON interviews(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_interviews_status ON interviews(status);