package ua.edu.ukma.candidai.vacancy.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ua.edu.ukma.candidai.vacancy.web.VacancyWebTestResources.*;

@WebMvcTest(controllers = VacancyWebController.class)
@Import({MethodSecurityConfiguration.class, TestSecurityConfiguration.class})
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
        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .param("title", VALID_FORM.title()))
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
        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .param("title", INVALID_FORM.title())
                        .param("specialization", INVALID_FORM.specialization())
                        .param("seniorityLevel", INVALID_FORM.seniorityLevel())
                        .param("minYearsOfExperience", INVALID_FORM.minYearsOfExperience().toString())
                        .param("description", INVALID_FORM.description())
                        .param("requiredSkills", INVALID_FORM.requiredSkills())
                        .param("salaryMin", INVALID_FORM.salaryMin().toString())
                        .param("salaryMax", INVALID_FORM.salaryMax().toString())
                        .param("currency", INVALID_FORM.currency()))
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
        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .param("title", INVALID_SALARY_RANGE_FORM.title())
                        .param("category", INVALID_SALARY_RANGE_FORM.category().name())
                        .param("specialization", INVALID_SALARY_RANGE_FORM.specialization())
                        .param("seniorityLevel", INVALID_SALARY_RANGE_FORM.seniorityLevel())
                        .param("minYearsOfExperience", INVALID_SALARY_RANGE_FORM.minYearsOfExperience().toString())
                        .param("description", INVALID_SALARY_RANGE_FORM.description())
                        .param("requiredSkills", INVALID_SALARY_RANGE_FORM.requiredSkills())
                        .param("preferredSkills", INVALID_SALARY_RANGE_FORM.preferredSkills())
                        .param("minEnglishLevel", INVALID_SALARY_RANGE_FORM.minEnglishLevel().name())
                        .param("salaryMin", INVALID_SALARY_RANGE_FORM.salaryMin().toString())
                        .param("salaryMax", INVALID_SALARY_RANGE_FORM.salaryMax().toString())
                        .param("currency", INVALID_SALARY_RANGE_FORM.currency())
                        .param("employmentType", INVALID_SALARY_RANGE_FORM.employmentType().name())
                        .param("locationType", INVALID_SALARY_RANGE_FORM.locationType().name())
                        .param("location", INVALID_SALARY_RANGE_FORM.location()))
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

        ArgumentCaptor<CreateVacancyRequest> captor = ArgumentCaptor.forClass(CreateVacancyRequest.class);

        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .param("title", VALID_FORM.title())
                        .param("category", VALID_FORM.category().name())
                        .param("specialization", VALID_FORM.specialization())
                        .param("seniorityLevel", VALID_FORM.seniorityLevel())
                        .param("minYearsOfExperience", VALID_FORM.minYearsOfExperience().toString())
                        .param("description", VALID_FORM.description())
                        .param("requiredSkills", VALID_FORM.requiredSkills())
                        .param("preferredSkills", VALID_FORM.preferredSkills())
                        .param("minEnglishLevel", VALID_FORM.minEnglishLevel().name())
                        .param("salaryMin", VALID_FORM.salaryMin().toString())
                        .param("salaryMax", VALID_FORM.salaryMax().toString())
                        .param("currency", VALID_FORM.currency())
                        .param("employmentType", VALID_FORM.employmentType().name())
                        .param("locationType", VALID_FORM.locationType().name())
                        .param("location", VALID_FORM.location()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_URL))
                .andExpect(flash().attribute("successMessage", "Vacancy successfully created!"));

        verify(vacancyService).createVacancy(captor.capture());

        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(EXPECTED_CREATE_REQUEST);
    }

    @Test
    @DisplayName("handleCreateVacancy - should redirect with Ukrainian flash message when uk locale is set")
    @WithMockUser(username = RECRUITER_EMAIL, roles = "RECRUITER")
    void givenUkrainianLocale_createVacancy_shouldRedirectWithUkrainianFlashMessage() throws Exception {
        when(userApi.getUserNotificationProfileByEmail(RECRUITER_EMAIL))
                .thenReturn(Optional.of(RECRUITER_PROFILE));

        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .locale(java.util.Locale.forLanguageTag("uk"))
                        .param("title", VALID_FORM.title())
                        .param("category", VALID_FORM.category().name())
                        .param("specialization", VALID_FORM.specialization())
                        .param("seniorityLevel", VALID_FORM.seniorityLevel())
                        .param("minYearsOfExperience", VALID_FORM.minYearsOfExperience().toString())
                        .param("description", VALID_FORM.description())
                        .param("requiredSkills", VALID_FORM.requiredSkills())
                        .param("preferredSkills", VALID_FORM.preferredSkills())
                        .param("minEnglishLevel", VALID_FORM.minEnglishLevel().name())
                        .param("salaryMin", VALID_FORM.salaryMin().toString())
                        .param("salaryMax", VALID_FORM.salaryMax().toString())
                        .param("currency", VALID_FORM.currency())
                        .param("employmentType", VALID_FORM.employmentType().name())
                        .param("locationType", VALID_FORM.locationType().name())
                        .param("location", VALID_FORM.location()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(VACANCIES_URL))
                .andExpect(flash().attribute("successMessage", "Вакансію успішно збережено!"));
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

        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .param("title", VALID_FORM.title())
                        .param("category", VALID_FORM.category().name())
                        .param("specialization", VALID_FORM.specialization())
                        .param("seniorityLevel", VALID_FORM.seniorityLevel())
                        .param("minYearsOfExperience", VALID_FORM.minYearsOfExperience().toString())
                        .param("description", VALID_FORM.description())
                        .param("requiredSkills", VALID_FORM.requiredSkills())
                        .param("preferredSkills", VALID_FORM.preferredSkills())
                        .param("minEnglishLevel", VALID_FORM.minEnglishLevel().name())
                        .param("salaryMin", VALID_FORM.salaryMin().toString())
                        .param("salaryMax", VALID_FORM.salaryMax().toString())
                        .param("currency", VALID_FORM.currency())
                        .param("employmentType", VALID_FORM.employmentType().name())
                        .param("locationType", VALID_FORM.locationType().name())
                        .param("location", VALID_FORM.location()))
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
        mockMvc.perform(post(VACANCIES_URL)
                        .with(csrf())
                        .param("title", VALID_FORM.title())
                        .param("category", VALID_FORM.category().name())
                        .param("specialization", VALID_FORM.specialization())
                        .param("seniorityLevel", VALID_FORM.seniorityLevel())
                        .param("minYearsOfExperience", VALID_FORM.minYearsOfExperience().toString())
                        .param("description", VALID_FORM.description())
                        .param("requiredSkills", "  ,  ,  ,  ")
                        .param("preferredSkills", VALID_FORM.preferredSkills())
                        .param("minEnglishLevel", VALID_FORM.minEnglishLevel().name())
                        .param("salaryMin", VALID_FORM.salaryMin().toString())
                        .param("salaryMax", VALID_FORM.salaryMax().toString())
                        .param("currency", VALID_FORM.currency())
                        .param("employmentType", VALID_FORM.employmentType().name())
                        .param("locationType", VALID_FORM.locationType().name())
                        .param("location", VALID_FORM.location()))
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
}
