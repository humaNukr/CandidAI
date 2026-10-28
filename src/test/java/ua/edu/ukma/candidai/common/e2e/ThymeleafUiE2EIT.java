package ua.edu.ukma.candidai.common.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import ua.edu.ukma.candidai.common.annotation.E2ETest;
import ua.edu.ukma.candidai.common.e2e.pages.ApplicationsListPage;
import ua.edu.ukma.candidai.common.e2e.pages.ApplyVacancyPage;
import ua.edu.ukma.candidai.common.e2e.pages.CreateVacancyPage;
import ua.edu.ukma.candidai.common.e2e.pages.LoginPage;
import ua.edu.ukma.candidai.common.e2e.pages.VacancyListPage;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;

@E2ETest
class ThymeleafUiE2EIT extends BrowserTestSupport {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("Scenario 1: Recruiter logs in, creates vacancy, and it appears in registry")
    void recruiterCreatesVacancySuccessfully() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate();
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

        assertThat(vacancyListPage.alert()).containsText("Вакансію успішно збережено!");
        assertThat(vacancyListPage.row(vacancyTitle)).isVisible();
    }

    @Test
    @DisplayName("Scenario 2 & 3: Candidate applies for vacancy and Tech Lead views evaluated application")
    void candidateAppliesAndTechLeadViewsApplication() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate();
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
        assertThat(vacancyListPage.row(vacancyTitle)).isVisible();
        vacancyListPage.logout();

        loginPage.login("candidate@candidai.com", "CandidatePass123!");
        vacancyListPage.navigate();
        vacancyListPage.clickApplyForVacancy(vacancyTitle);

        ApplyVacancyPage applyPage = new ApplyVacancyPage(page);
        applyPage.fillForm(
                "Oksana Kovalenko",
                "candidate@candidai.com",
                "+380509876543",
                "https://storage.candidai.ukma.edu.ua/cv/oksana.pdf"
        );
        applyPage.submit();

        assertThat(vacancyListPage.alert()).containsText("Вашу заявку успішно надіслано!");
        await().atMost(Duration.ofSeconds(5)).until(() ->
                jdbcClient.sql("SELECT status FROM applications WHERE email = :email")
                        .param("email", "candidate@candidai.com")
                        .query(String.class)
                        .optional()
                        .map(status -> ApplicationStatus.SCREENING.name().equals(status))
                        .orElse(false)
        );
        vacancyListPage.logout();

        loginPage.login("techlead@candidai.com", "TechLeadPass123!");
        vacancyListPage.navigate();
        vacancyListPage.clickViewApplicationsForVacancy(vacancyTitle);

        ApplicationsListPage applicationsListPage = new ApplicationsListPage(page);
        Locator row = applicationsListPage.candidateRow("candidate@candidai.com");
        assertThat(row).containsText("Oksana Kovalenko");
        assertThat(row).containsText("SCREENING");
        assertThat(row).containsText("88%");
    }

    @Test
    @DisplayName("Scenario 4: Candidate does not see create button and gets 403 Forbidden on /vacancies/new")
    void candidateCannotAccessCreateVacancyPage() {
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate();
        loginPage.login("candidate@candidai.com", "CandidatePass123!");

        VacancyListPage vacancyListPage = new VacancyListPage(page);
        assertThat(vacancyListPage.createVacancyLink()).hasCount(0);

        CreateVacancyPage createVacancyPage = new CreateVacancyPage(page);
        createVacancyPage.navigate();

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Доступ заборонено")))
                .isVisible();
    }
}
