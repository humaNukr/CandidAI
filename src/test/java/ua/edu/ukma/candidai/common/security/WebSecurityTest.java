package ua.edu.ukma.candidai.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:websectestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class WebSecurityTest {

    private static final String LOGIN_PATH = "/login";
    private static final String VACANCIES_PATH = "/vacancies";
    private static final String CSP_HEADER = "Content-Security-Policy";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Root URL redirects to /vacancies")
    void rootUrl_shouldRedirectToVacancies() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_PATH));
    }

    @Test
    @DisplayName("Login page returns 200 OK with CSRF token and strict CSP headers")
    void loginPage_whenAnonymous_shouldReturn200WithCsrfAndCsp() throws Exception {
        mockMvc.perform(get(LOGIN_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(header().string(CSP_HEADER, containsString("frame-ancestors 'none'")))
                .andExpect(header().string(CSP_HEADER, containsString("script-src 'self'")));
    }

    @Test
    @DisplayName("Form login with valid credentials authenticates and redirects to vacancies")
    void login_withValidCredentials_shouldAuthenticateAndRedirect() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .with(csrf())
                        .param("username", "recruiter@candidai.com")
                        .param("password", "RecruiterPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_PATH));
    }

    @Test
    @DisplayName("Form login with invalid password redirects to /login?error")
    void login_withInvalidCredentials_shouldRedirectToLoginError() throws Exception {
        mockMvc.perform(post(LOGIN_PATH)
                        .with(csrf())
                        .param("username", "recruiter@candidai.com")
                        .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Logout invalidates session and redirects to /login?logout")
    void logout_whenAuthenticated_shouldInvalidateAndRedirect() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(csrf())
                        .with(user("recruiter@candidai.com").roles("RECRUITER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("POST request without CSRF token is rejected with 403 Forbidden")
    void post_withoutCsrf_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(post(VACANCIES_PATH)
                        .with(user("recruiter@candidai.com").roles("RECRUITER"))
                        .param("title", "New Vacancy"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OpenAPI docs are served with relaxed CSP enabling inline scripts for Swagger UI")
    void openApiDocs_shouldHaveRelaxedCsp() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(header().string(CSP_HEADER, containsString("'unsafe-inline'")));
    }

    @Test
    @DisplayName("Accessing /admin/** with non-admin role is rejected with 403 Forbidden")
    void adminEndpoint_whenCandidateRole_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(user("candidate@candidai.com").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /vacancies/new without authentication redirects to /login")
    void getVacanciesNew_whenAnonymous_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/vacancies/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(LOGIN_PATH));
    }

    @Test
    @DisplayName("GET /vacancies/new when candidate role returns 403 Forbidden")
    void getVacanciesNew_whenCandidateRole_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/vacancies/new")
                        .with(user("candidate@candidai.com").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /vacancies/new when admin role is authorized")
    void getVacanciesNew_whenAdminRole_shouldNotBeForbidden() throws Exception {
        mockMvc.perform(get("/vacancies/new")
                        .with(user("admin@candidai.com").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /vacancies when candidate role returns 403 Forbidden")
    void postVacancies_whenCandidateRole_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(post(VACANCIES_PATH)
                        .with(csrf())
                        .with(user("candidate@candidai.com").roles("CANDIDATE"))
                        .param("title", "Java Engineer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /vacancies when admin role with CSRF is authorized")
    void postVacancies_whenAdminRoleWithCsrf_shouldNotBeForbidden() throws Exception {
        mockMvc.perform(post(VACANCIES_PATH)
                        .with(csrf())
                        .with(user("admin@candidai.com").roles("ADMIN"))
                        .param("title", "Java Engineer"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /vacancies/*/apply allows candidate role with CSRF")
    void postVacanciesApply_whenCandidateWithCsrf_shouldNotBeForbiddenByRecruiterRule() throws Exception {
        mockMvc.perform(post("/vacancies/123/apply")
                        .with(csrf())
                        .with(user("candidate@candidai.com").roles("CANDIDATE"))
                        .param("candidateName", "John Doe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /vacancies/*/apply rejects non-candidate roles with 403 Forbidden")
    void postVacanciesApply_whenRecruiterRole_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(post("/vacancies/123/apply")
                        .with(csrf())
                        .with(user("recruiter@candidai.com").roles("RECRUITER"))
                        .param("candidateName", "Jane Recruiter"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /vacancies/*/applications rejects candidate role with 403 Forbidden")
    void getVacanciesApplications_whenCandidateRole_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/vacancies/123/applications")
                        .with(user("candidate@candidai.com").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /vacancies/*/applications authorizes tech lead role")
    void getVacanciesApplications_whenTechLeadRole_shouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/vacancies/123/applications")
                        .with(user("techlead@candidai.com").roles("TECH_LEAD")))
                .andExpect(status().isNotFound());
    }
}
