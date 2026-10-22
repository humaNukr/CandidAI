package ua.edu.ukma.candidai.vacancy.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
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
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;
import ua.edu.ukma.candidai.vacancy.web.dto.CreateVacancyForm;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.vacancy.web.VacancyWebTestResources.*;

@WebMvcTest(controllers = VacancyWebController.class)
@Import({MethodSecurityConfiguration.class, TestSecurityConfiguration.class, WebExceptionAdvice.class})
@EnableSpringDataWebSupport
class VacancyWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VacancyService vacancyService;

    @MockitoBean
    private UserApi userApi;

    @Test
    @DisplayName("listVacancies - should render table view by default")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenDefaultParams_getVacancies_shouldRenderTableListView() throws Exception {
        when(vacancyService.getAllVacancies(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(SINGLE_PAGE_VACANCIES);

        mockMvc.perform(get(VACANCIES_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("vacancies/list"))
                .andExpect(model().attributeExists("vacancies", "statuses", "categories", "views"))
                .andExpect(model().attribute("selectedView", ua.edu.ukma.candidai.vacancy.web.model.VacancyView.TABLE));
    }

    @Test
    @DisplayName("listVacancies - should render cards view when view=CARDS")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenCardsViewParam_getVacancies_shouldRenderCardsListView() throws Exception {
        when(vacancyService.getAllVacancies(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(SINGLE_PAGE_VACANCIES);

        mockMvc.perform(get(VACANCIES_URL).param("view", "CARDS"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacancies/cards"))
                .andExpect(model().attribute("selectedView", ua.edu.ukma.candidai.vacancy.web.model.VacancyView.CARDS));
    }

    @Test
    @DisplayName("listVacancies - should reject invalid view name with 400 Bad Request to prevent SSTI")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenUnknownViewParam_getVacancies_shouldRejectWithBadRequest() throws Exception {
        mockMvc.perform(get(VACANCIES_URL).param("view", "malicious__${T(java.lang.Runtime).getRuntime()}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("showCreateForm - should render create form for RECRUITER")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenRecruiterRole_getCreateForm_shouldRenderFormWithReferenceData() throws Exception {
        mockMvc.perform(get(NEW_VACANCY_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("vacancies/create-form"))
                .andExpect(model().attributeExists(
                        "form", "categories", "englishLevels", "employmentTypes", "locationTypes"
                ));
    }

    @Test
    @DisplayName("showCreateForm - should return 403 Forbidden for CANDIDATE")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_getCreateForm_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get(NEW_VACANCY_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("handleCreateVacancy - should return 403 Forbidden for CANDIDATE")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_createVacancy_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(postVacancyForm(VACANCIES_URL, VALID_FORM))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("showCreateForm - should return 403 Forbidden for anonymous user")
    @WithAnonymousUser
    void givenAnonymousUser_getCreateForm_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get(NEW_VACANCY_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("handleCreateVacancy - should return 422 Unprocessable Entity when validation fails")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenInvalidForm_createVacancy_shouldReturn422WithFieldErrors() throws Exception {
        mockMvc.perform(postVacancyForm(VACANCIES_URL, INVALID_FORM))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("vacancies/create-form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors(
                        "form",
                        "title",
                        "category",
                        "specialization",
                        "seniorityLevel",
                        "description",
                        "requiredSkills",
                        "minEnglishLevel",
                        "salaryMin",
                        "salaryMax",
                        "currency",
                        "employmentType",
                        "locationType"
                ));
    }

    @Test
    @DisplayName("handleCreateVacancy - should return 422 when salaryMax is less than salaryMin")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenInvalidSalaryRange_createVacancy_shouldReturn422WithSalaryError() throws Exception {
        mockMvc.perform(postVacancyForm(VACANCIES_URL, INVALID_SALARY_RANGE_FORM))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("vacancies/create-form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("form", "salaryMin"));
    }

    @Test
    @DisplayName("handleCreateVacancy - should redirect to /vacancies with flash message and persist via service (PRG)")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenValidForm_createVacancy_shouldRedirectToVacanciesAndPersist() throws Exception {
        when(userApi.getUserNotificationProfileByEmail(RECRUITER_EMAIL))
                .thenReturn(Optional.of(RECRUITER_PROFILE));

        mockMvc.perform(postVacancyForm(VACANCIES_URL, VALID_FORM).cookie(new Cookie("lang", "en")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_URL))
                .andExpect(flash().attribute("successMessage", "Vacancy successfully created!"));

        verify(vacancyService).createVacancy(EXPECTED_CREATE_REQUEST);
    }

    @Test
    @DisplayName("handleCreateVacancy - should redirect with Ukrainian flash message when uk locale is set")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenUkrainianLocale_createVacancy_shouldRedirectWithUkrainianFlashMessage() throws Exception {
        when(userApi.getUserNotificationProfileByEmail(RECRUITER_EMAIL))
                .thenReturn(Optional.of(RECRUITER_PROFILE));

        mockMvc.perform(postVacancyForm(VACANCIES_URL, VALID_FORM).cookie(new Cookie("lang", "uk")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_URL))
                .andExpect(flash().attribute("successMessage", "Вакансію успішно збережено!"));
    }

    @Test
    @DisplayName("handleCreateVacancy - should return 422 with title error when DuplicateResourceException is thrown")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenDuplicateTitle_createVacancy_shouldReturn422WithTitleError() throws Exception {
        when(userApi.getUserNotificationProfileByEmail(RECRUITER_EMAIL))
                .thenReturn(Optional.of(RECRUITER_PROFILE));
        when(vacancyService.createVacancy(any(CreateVacancyRequest.class)))
                .thenThrow(new DuplicateResourceException("Active vacancy already exists"));

        mockMvc.perform(postVacancyForm(VACANCIES_URL, VALID_FORM).cookie(new Cookie("lang", "uk")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("vacancies/create-form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("form", "title"));
    }

    @Test
    @DisplayName("listVacancies - should render dynamic IDs on action buttons in table view")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_listVacancies_shouldRenderDynamicApplyButtonId() throws Exception {
        when(vacancyService.getAllVacancies(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(SINGLE_PAGE_VACANCIES);

        MvcResult result = mockMvc.perform(get(VACANCIES_URL))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("id=\"btn-apply-" + VACANCY_RESPONSE_1.id() + "\"")
                .doesNotContain("id=\"btn-apply\"");
    }

    @Test
    @DisplayName("handleCreateVacancy - should return 422 when recruiter profile not found")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenRecruiterProfileNotFound_createVacancy_shouldReturn422WithGlobalError() throws Exception {
        when(userApi.getUserNotificationProfileByEmail(RECRUITER_EMAIL))
                .thenReturn(Optional.empty());

        mockMvc.perform(postVacancyForm(VACANCIES_URL, VALID_FORM))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("vacancies/create-form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasErrors("form"));

        verify(vacancyService, never()).createVacancy(any());
    }

    @Test
    @DisplayName("handleCreateVacancy - should return 422 when requiredSkills contains only commas and spaces")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenSkillsOnlyCommasAndSpaces_createVacancy_shouldReturn422WithSkillsError() throws Exception {
        CreateVacancyForm formWithBlankSkills = VALID_FORM.toBuilder()
                .requiredSkills("  ,  ,  ,  ")
                .build();

        mockMvc.perform(postVacancyForm(VACANCIES_URL, formWithBlankSkills))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("vacancies/create-form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("form", "requiredSkills"));
    }

    @Test
    @DisplayName("listVacancies - should render error message alert when flash attribute is present")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenErrorMessageInFlash_getVacancies_shouldRenderErrorAlert() throws Exception {
        when(vacancyService.getAllVacancies(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(SINGLE_PAGE_VACANCIES);

        MvcResult result = mockMvc.perform(get(VACANCIES_URL).flashAttr("errorMessage", "Не вдалося зберегти дані"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Не вдалося зберегти дані"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("Не вдалося зберегти дані")
                .contains("alert-danger");
    }

    @Test
    @DisplayName("listVacancies - should render XSS payload safely escaped in HTML")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenXssPayloadInList_getVacancies_shouldEscapeHtmlInResponse() throws Exception {
        when(vacancyService.getAllVacancies(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(XSS_PAGE_VACANCIES);

        MvcResult result = mockMvc.perform(get(VACANCIES_URL))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html)
                .contains("&lt;script&gt;alert(&#39;title&#39;)&lt;/script&gt;")
                .doesNotContain("<script>alert('title')</script>");
    }

    @Test
    @DisplayName("showVacancy - should redirect to /vacancies when vacancy exists")
    @WithAnonymousUser
    void givenExistingId_showVacancy_shouldRedirectToVacancies() throws Exception {
        UUID vacancyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(vacancyService.getVacancyById(vacancyId)).thenReturn(VACANCY_RESPONSE_1);

        mockMvc.perform(get(VACANCIES_URL + "/" + vacancyId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_URL));
    }

    @Test
    @DisplayName("showVacancy - should return 404 Not Found view when vacancy does not exist")
    @WithAnonymousUser
    void givenNonExistentId_showVacancy_shouldReturn404View() throws Exception {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        when(vacancyService.getVacancyById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Vacancy not found"));

        mockMvc.perform(get(VACANCIES_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("showVacancy - should return 404 Not Found view when id is invalid UUID")
    @WithAnonymousUser
    void givenInvalidUuid_showVacancy_shouldReturn404View() throws Exception {
        mockMvc.perform(get(VACANCIES_URL + "/invalid-uuid-string"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("showCreateForm - should render placeholders in English when English locale requested")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenEnglishLocale_getCreateForm_shouldRenderEnglishPlaceholders() throws Exception {
        MvcResult result = mockMvc.perform(get(NEW_VACANCY_URL).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacancies/create-form"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html)
                .contains("placeholder=\"e.g. Senior Java Developer\"")
                .contains("placeholder=\"e.g. Java / Spring Boot\"")
                .contains("placeholder=\"e.g. Senior or Lead\"")
                .contains("placeholder=\"e.g. 4000\"")
                .contains("placeholder=\"Kyiv, Ukraine\"");
    }

    @Test
    @DisplayName("showCreateForm - should render placeholders in Ukrainian when Ukrainian locale requested")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenUkrainianLocale_getCreateForm_shouldRenderUkrainianPlaceholders() throws Exception {
        MvcResult result = mockMvc.perform(get(NEW_VACANCY_URL).param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(view().name("vacancies/create-form"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html)
                .contains("placeholder=\"напр. Senior Java Developer\"")
                .contains("placeholder=\"напр. Java / Spring Boot\"")
                .contains("placeholder=\"напр. Senior або Lead\"")
                .contains("placeholder=\"напр. 4000\"")
                .contains("placeholder=\"Київ, Україна\"");
    }

    @Test
    @DisplayName("toRequest - should map form fields to CreateVacancyRequest")
    void givenValidForm_toRequest_shouldMapAllFields() {
        CreateVacancyRequest actual = VALID_FORM.toRequest(RECRUITER_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(EXPECTED_CREATE_REQUEST);
    }
}
