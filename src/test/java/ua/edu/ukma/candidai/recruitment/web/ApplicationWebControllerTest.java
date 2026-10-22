package ua.edu.ukma.candidai.recruitment.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.edu.ukma.candidai.common.config.MethodSecurityConfiguration;
import ua.edu.ukma.candidai.common.config.TestSecurityConfiguration;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.exception.WebExceptionAdvice;
import ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.response.ApplicationResponse;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ApplicationWebController.class)
@Import({MethodSecurityConfiguration.class, TestSecurityConfiguration.class, WebExceptionAdvice.class})
class ApplicationWebControllerTest {

    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CANDIDATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String APPLY_URL = "/vacancies/" + VACANCY_ID + "/apply";
    private static final String APPLICATIONS_URL = "/vacancies/" + VACANCY_ID + "/applications";
    private static final String RESUME_URL = "https://storage.candidai.com/resume.pdf";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private UserApi userApi;

    @MockitoBean
    private VacancyApi vacancyApi;

    @Test
    @DisplayName("showApplyForm - should return 200 OK with apply form when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_showApplyForm_shouldReturnOk() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(
                new VacancyDetails(VACANCY_ID, "Java Developer", "Description", List.of("Java"), List.of(), "Senior")
        );
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(
                Optional.of(new UserNotificationProfile(CANDIDATE_ID, "Jane Doe", "candidate@candidai.com", null))
        );

        mockMvc.perform(get(APPLY_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/apply-form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("vacancyTitle", "Java Developer"));
    }

    @Test
    @DisplayName("showApplyForm - should return 403 Forbidden when called by recruiter")
    @WithMockUser(username = "recruiter@candidai.com", roles = "RECRUITER")
    void givenRecruiterRole_showApplyForm_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get(APPLY_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("submitApplication - should return form view with validation errors when invalid input")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenInvalidForm_submitApplication_shouldReturnFormWithErrors() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(
                new VacancyDetails(VACANCY_ID, "Java Developer", "Description", List.of(), List.of(), "Senior")
        );

        mockMvc.perform(postApplyForm("", "not-an-email", null, "not-a-url"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/apply-form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("submitApplication - should redirect to /vacancies with success message when valid input")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenValidForm_submitApplication_shouldRedirectWithSuccess() throws Exception {
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(
                Optional.of(new UserNotificationProfile(CANDIDATE_ID, "Jane Doe", "candidate@candidai.com", null))
        );
        when(applicationService.apply(any(ApplyForVacancyRequest.class))).thenReturn(
                new ApplicationResponse(
                        UUID.randomUUID(), VACANCY_ID, CANDIDATE_ID, "Jane Doe",
                        "candidate@candidai.com", "+380501234567", RESUME_URL,
                        ApplicationStatus.APPLIED, null, null, Instant.now(), Instant.now()
                )
        );

        mockMvc.perform(postApplyForm("Jane Doe", "candidate@candidai.com", "+380501234567", RESUME_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vacancies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(applicationService).apply(new ApplyForVacancyRequest(
                VACANCY_ID,
                CANDIDATE_ID,
                "Jane Doe",
                "candidate@candidai.com",
                "+380501234567",
                RESUME_URL
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+380 50 123 45 67",
            "+380 (50) 123-45-67",
            "+1 (555) 123-4567",
            "+44 20 7946 0958",
            "+48 12 345 67 89"
    })
    @DisplayName("submitApplication - should accept valid international phone numbers")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenInternationalPhone_submitApplication_shouldRedirectWithSuccess(String phone) throws Exception {
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(
                Optional.of(new UserNotificationProfile(CANDIDATE_ID, "Jane Doe", "candidate@candidai.com", null))
        );
        when(applicationService.apply(any(ApplyForVacancyRequest.class))).thenReturn(
                new ApplicationResponse(
                        UUID.randomUUID(), VACANCY_ID, CANDIDATE_ID, "Jane Doe",
                        "candidate@candidai.com", phone, RESUME_URL,
                        ApplicationStatus.APPLIED, null, null, Instant.now(), Instant.now()
                )
        );

        mockMvc.perform(postApplyForm("Jane Doe", "candidate@candidai.com", phone, RESUME_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vacancies"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc12345",
            "123",
            "++380501234567",
            "+12345678901234567890"
    })
    @DisplayName("submitApplication - should return form view with errors when phone is invalid")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenInvalidPhone_submitApplication_shouldReturnFormWithErrors(String invalidPhone) throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(
                new VacancyDetails(VACANCY_ID, "Java Developer", "Description", List.of(), List.of(), "Senior")
        );

        mockMvc.perform(postApplyForm("Jane Doe", "candidate@candidai.com", invalidPhone, RESUME_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/apply-form"))
                .andExpect(model().attributeHasFieldErrors("form", "phone"));
    }

    @Test
    @DisplayName("submitApplication - should redirect with error message when duplicate application")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenDuplicateApplication_submitApplication_shouldRedirectWithError() throws Exception {
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(
                Optional.of(new UserNotificationProfile(CANDIDATE_ID, "Jane Doe", "candidate@candidai.com", null))
        );
        when(applicationService.apply(any(ApplyForVacancyRequest.class)))
                .thenThrow(new DuplicateResourceException("Candidate already applied to this vacancy"));

        mockMvc.perform(postApplyForm("Jane Doe", "candidate@candidai.com", "+380501234567", RESUME_URL)
                        .cookie(new Cookie("lang", "uk")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vacancies"))
                .andExpect(flash().attribute("errorMessage", "Ви вже подавали заявку на цю вакансію."));
    }

    private static MockHttpServletRequestBuilder postApplyForm(
            String name, String email, String phone, String resumeUrl) {
        MockHttpServletRequestBuilder builder = post(APPLY_URL).with(csrf());
        if (name != null) builder.param("candidateName", name);
        if (email != null) builder.param("email", email);
        if (phone != null) builder.param("phone", phone);
        if (resumeUrl != null) builder.param("resumeUrl", resumeUrl);
        return builder;
    }

    @Test
    @DisplayName("viewApplications - should return 200 OK with applications list when called by recruiter")
    @WithMockUser(username = "recruiter@candidai.com", roles = "RECRUITER")
    void givenRecruiterRole_viewApplications_shouldReturnOk() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(
                new VacancyDetails(VACANCY_ID, "Java Developer", "Description", List.of(), List.of(), "Senior")
        );
        when(applicationService.getApplicationsByVacancy(VACANCY_ID, true)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(APPLICATIONS_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/list"))
                .andExpect(model().attributeExists("applications"));
    }

    @Test
    @DisplayName("viewApplications - should return 200 OK with applications list when called by tech lead")
    @WithMockUser(username = "techlead@candidai.com", roles = "TECH_LEAD")
    void givenTechLeadRole_viewApplications_shouldReturnOk() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(
                new VacancyDetails(VACANCY_ID, "Java Developer", "Description", List.of(), List.of(), "Senior")
        );
        when(applicationService.getApplicationsByVacancy(VACANCY_ID, true)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(APPLICATIONS_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/list"))
                .andExpect(model().attributeExists("applications"));
    }

    @Test
    @DisplayName("viewApplications - should return 403 Forbidden when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_viewApplications_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get(APPLICATIONS_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("viewApplications - should return 403 Forbidden for anonymous user")
    @WithAnonymousUser
    void givenAnonymousUser_viewApplications_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get(APPLICATIONS_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("showApplyForm - should return 404 Not Found when vacancy does not exist")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenNonExistentVacancy_showApplyForm_shouldReturn404() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID))
                .thenThrow(new ResourceNotFoundException("Vacancy not found"));

        mockMvc.perform(get(APPLY_URL))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("viewApplications - should return 404 Not Found when vacancy does not exist")
    @WithMockUser(username = "techlead@candidai.com", roles = "TECH_LEAD")
    void givenNonExistentVacancy_viewApplications_shouldReturn404() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID))
                .thenThrow(new ResourceNotFoundException("Vacancy not found"));

        mockMvc.perform(get(APPLICATIONS_URL))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("showApplyForm - should render placeholders in English when English locale requested")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenEnglishLocale_getApplyForm_shouldRenderEnglishPlaceholders() throws Exception {
        UserNotificationProfile profile = new UserNotificationProfile(
                CANDIDATE_ID, "John Doe", "candidate@candidai.com", null
        );
        VacancyDetails details = new VacancyDetails(
                VACANCY_ID, "Java Engineer", "Description", List.of("Java"), List.of(), "Senior"
        );
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(Optional.of(profile));
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(details);

        MvcResult result = mockMvc.perform(get(APPLY_URL).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/apply-form"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html)
                .contains("placeholder=\"John Doe\"")
                .contains("placeholder=\"candidate@example.com\"")
                .contains("placeholder=\"+1 555 123-4567\"")
                .contains("placeholder=\"https://storage.candidai.com/resumes/my_resume.pdf\"");
    }

    @Test
    @DisplayName("showApplyForm - should render placeholders in Ukrainian when Ukrainian locale requested")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenUkrainianLocale_getApplyForm_shouldRenderUkrainianPlaceholders() throws Exception {
        UserNotificationProfile profile = new UserNotificationProfile(
                CANDIDATE_ID, "Іван Петренко", "candidate@candidai.com", null
        );
        VacancyDetails details = new VacancyDetails(
                VACANCY_ID, "Java Engineer", "Description", List.of("Java"), List.of(), "Senior"
        );
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(Optional.of(profile));
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(details);

        MvcResult result = mockMvc.perform(get(APPLY_URL).param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/apply-form"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html)
                .contains("placeholder=\"Іван Петренко\"")
                .contains("placeholder=\"candidate@example.com\"")
                .contains("placeholder=\"+380 50 123 45 67\"")
                .contains("placeholder=\"https://storage.candidai.com/resumes/my_resume.pdf\"");
    }
}

