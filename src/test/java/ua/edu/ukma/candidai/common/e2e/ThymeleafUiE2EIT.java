package ua.edu.ukma.candidai.common.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.common.e2e.pages.ApplicationsListPage;
import ua.edu.ukma.candidai.common.e2e.pages.ApplyVacancyPage;
import ua.edu.ukma.candidai.common.e2e.pages.CreateVacancyPage;
import ua.edu.ukma.candidai.common.e2e.pages.LoginPage;
import ua.edu.ukma.candidai.common.e2e.pages.VacancyListPage;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafUiE2EIT extends BrowserTestSupport {

    @Test
    @DisplayName("Scenario 1: Recruiter logs in, creates vacancy, and it appears in registry")
    void recruiterCreatesVacancySuccessfully() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate(baseUrl());
        loginPage.login("recruiter@candidai.com", "RecruiterPass123!");

        VacancyListPage vacancyListPage = new VacancyListPage(page);
        vacancyListPage.clickCreateVacancy();

        CreateVacancyPage createVacancyPage = new CreateVacancyPage(page);
        String vacancyTitle = "Staff AI Platform Engineer";
        createVacancyPage.fillForm(
                vacancyTitle,
                "ENGINEERING",
                "Machine Learning / Distributed Systems",
                "Staff",
                "8",
                "Java 25, Spring Boot, PyTorch, Kubernetes",
                "7000",
                "9500",
                "Leading architectural design of scalable AI candidate screening models."
        );
        createVacancyPage.submit();

        assertThat(vacancyListPage.hasSuccessAlert()).isTrue();
        assertThat(vacancyListPage.hasVacancyWithTitle(vacancyTitle)).isTrue();
        captureScreenshot("01-recruiter-created-vacancy.png");
    }

    @Test
    @DisplayName("Scenario 2 & 3: Candidate applies for vacancy and Tech Lead views applications")
    void candidateAppliesAndTechLeadViewsApplication() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate(baseUrl());
        loginPage.login("recruiter@candidai.com", "RecruiterPass123!");

        VacancyListPage vacancyListPage = new VacancyListPage(page);
        vacancyListPage.clickCreateVacancy();

        CreateVacancyPage createVacancyPage = new CreateVacancyPage(page);
        String vacancyTitle = "Senior Backend Architect";
        createVacancyPage.fillForm(
                vacancyTitle,
                "ENGINEERING",
                "Spring Boot",
                "Senior",
                "5",
                "Java, Spring, Postgres",
                "4000",
                "6000",
                "Architecting mission critical backend microservices."
        );
        createVacancyPage.submit();
        vacancyListPage.logout();

        loginPage.login("candidate@candidai.com", "CandidatePass123!");
        vacancyListPage.navigate(baseUrl());
        vacancyListPage.clickApplyForVacancy(vacancyTitle);

        ApplyVacancyPage applyPage = new ApplyVacancyPage(page);
        applyPage.fillForm(
                "Oksana Kovalenko",
                "candidate@candidai.com",
                "+380509876543",
                "https://storage.candidai.ukma.edu.ua/cv/oksana.pdf"
        );
        applyPage.submit();

        assertThat(vacancyListPage.hasSuccessAlert()).isTrue();
        captureScreenshot("02-candidate-applied-vacancy.png");
        vacancyListPage.logout();

        loginPage.login("techlead@candidai.com", "TechLeadPass123!");
        vacancyListPage.navigate(baseUrl());
        vacancyListPage.clickViewApplicationsForVacancy(vacancyTitle);

        ApplicationsListPage applicationsListPage = new ApplicationsListPage(page);
        assertThat(applicationsListPage.hasCandidateApplication("candidate@candidai.com")).isTrue();
        assertThat(applicationsListPage.getCandidateStatus("candidate@candidai.com")).isEqualTo("APPLIED");
        captureScreenshot("03-techlead-applications-list.png");
    }

    @Test
    @DisplayName("Scenario 4: Candidate attempts to access /vacancies/new and gets 403 Forbidden")
    void candidateCannotAccessCreateVacancyPage() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate(baseUrl());
        loginPage.login("candidate@candidai.com", "CandidatePass123!");

        CreateVacancyPage createVacancyPage = new CreateVacancyPage(page);
        createVacancyPage.navigate(baseUrl());

        assertThat(page.locator("h1.text-danger").isVisible()).isTrue();
        assertThat(page.locator("h1.text-danger").innerText())
                .containsAnyOf("Доступ заборонено", "Access Denied");
        captureScreenshot("04-candidate-access-denied-403.png");
    }
}
