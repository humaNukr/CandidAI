-- 1. Користувачі
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    telegram_chat_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Компанії (Батьківська для вакансій)
CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(255),
    contact_email VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. Вакансії (Належать компанії)
CREATE TABLE IF NOT EXISTS vacancies (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    author_id UUID,
    assigned_recruiter_id UUID,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    specialization VARCHAR(100),
    seniority_level VARCHAR(50),
    min_years_of_experience INT DEFAULT 0,
    description TEXT,
    min_english_level VARCHAR(20),
    salary_min NUMERIC(12, 2),
    salary_max NUMERIC(12, 2),
    currency VARCHAR(10) DEFAULT 'USD',
    employment_type VARCHAR(30),
    location_type VARCHAR(30),
    location VARCHAR(150),
    status VARCHAR(30) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_vacancy_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_vacancy_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 4. Навички (Для ManyToMany зв'язку з вакансіями)
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS vacancy_skills (
    vacancy_id UUID NOT NULL,
    skill_id UUID NOT NULL,
    PRIMARY KEY (vacancy_id, skill_id),
    CONSTRAINT fk_vs_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies(id) ON DELETE CASCADE,
    CONSTRAINT fk_vs_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- 5. Заявки кандидатів
CREATE TABLE IF NOT EXISTS applications (
    id UUID PRIMARY KEY,
    vacancy_id UUID NOT NULL,
    candidate_id UUID,
    candidate_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    resume_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    comment TEXT,
    matching_score INT,
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_app_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_candidate FOREIGN KEY (candidate_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 6. Відгуки співбесід (Належать заявці, OneToMany з orphanRemoval)
CREATE TABLE IF NOT EXISTS interview_feedbacks (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    interviewer_id UUID,
    interviewer_name VARCHAR(120) NOT NULL,
    technical_score INT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    notes TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_feedback_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_interviewer FOREIGN KEY (interviewer_id) REFERENCES users(id) ON DELETE SET NULL
);


-- 7. Співбесіди (Призначення зустрічей з кандидатами)
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