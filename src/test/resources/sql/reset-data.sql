TRUNCATE TABLE 
    interview_feedbacks, 
    interviews, 
    applications, 
    vacancy_skills, 
    vacancies 
RESTART IDENTITY CASCADE;

DELETE FROM companies 
WHERE id != 'c0000000-0000-0000-0000-000000000003';

DELETE FROM users 
WHERE id NOT IN (
    'e0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001', 
    'c0000000-0000-0000-0000-000000000001'
)
AND email NOT IN (
    'admin@candidai.com',
    'recruiter@candidai.com',
    'techlead@candidai.com',
    'candidate@candidai.com'
);
