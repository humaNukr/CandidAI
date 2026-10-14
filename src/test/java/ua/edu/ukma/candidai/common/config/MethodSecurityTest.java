package ua.edu.ukma.candidai.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.assessment.controller.AssessmentController;
import ua.edu.ukma.candidai.assessment.dto.AiScreeningResult;
import ua.edu.ukma.candidai.assessment.service.AssessmentService;
import ua.edu.ukma.candidai.company.controller.CompanyController;
import ua.edu.ukma.candidai.company.service.CompanyService;
import ua.edu.ukma.candidai.interview.controller.InterviewController;
import ua.edu.ukma.candidai.interview.dto.CancelInterviewRequest;
import ua.edu.ukma.candidai.interview.dto.InterviewResponse;
import ua.edu.ukma.candidai.interview.model.InterviewStatus;
import ua.edu.ukma.candidai.interview.service.InterviewService;
import ua.edu.ukma.candidai.recruitment.controller.ApplicationController;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.user.controller.UserController;
import ua.edu.ukma.candidai.user.service.UserService;
import ua.edu.ukma.candidai.vacancy.controller.VacancyController;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.ukma.candidai.recruitment.RecruitmentTestResources.anApplicationResponse;

@WebMvcTest(controllers = {
        CompanyController.class,
        VacancyController.class,
        ApplicationController.class,
        UserController.class,
        InterviewController.class,
        AssessmentController.class
})
@Import({MethodSecurityConfiguration.class, TestSecurityConfiguration.class})
class MethodSecurityTest {

    private static final String COMPANY_DELETE_URL
            = "/api/v1/companies/00000000-0000-0000-0000-000000000001";
    private static final String VACANCY_STATUS_URL
            = "/api/v1/vacancies/00000000-0000-0000-0000-000000000001/status";
    private static final String APPLICATION_STATUS_URL
            = "/api/v1/applications/00000000-0000-0000-0000-000000000001/status";
    private static final String USER_DELETE_URL
            = "/api/v1/users/00000000-0000-0000-0000-000000000001";
    private static final String INTERVIEW_CANCEL_URL
            = "/api/v1/interviews/00000000-0000-0000-0000-000000000001/cancel";
    private static final String ASSESSMENT_SCREENING_URL
            = "/api/v1/applications/00000000-0000-0000-0000-000000000001/screening";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private VacancyService vacancyService;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InterviewService interviewService;

    @MockitoBean
    private AssessmentService assessmentService;

    @Test
    @DisplayName("deleteCompany - should return 403 Forbidden when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_deleteCompany_shouldReturnForbidden() throws Exception {
        mockMvc.perform(delete(COMPANY_DELETE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteCompany - should return 204 NoContent when called by admin")
    @WithMockUser(username = "admin@candidai.com", roles = "ADMIN")
    void givenAdminRole_deleteCompany_shouldReturnNoContent() throws Exception {
        doNothing().when(companyService).deleteCompany(any(UUID.class));

        mockMvc.perform(delete(COMPANY_DELETE_URL))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("updateVacancyStatus - should return 403 Forbidden when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_updateVacancyStatus_shouldReturnForbidden() throws Exception {
        mockMvc.perform(patch(VACANCY_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateApplicationStatus - should return 403 Forbidden when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_updateApplicationStatus_shouldReturnForbidden() throws Exception {
        mockMvc.perform(patch(APPLICATION_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("updateApplicationStatus - should return 200 Ok when called by recruiter")
    @WithMockUser(username = "recruiter@candidai.com", roles = "RECRUITER")
    void givenRecruiterRole_updateApplicationStatus_shouldReturnOk() throws Exception {
        when(applicationService.updateStatus(any(UUID.class), any(UpdateApplicationStatusRequest.class)))
                .thenReturn(anApplicationResponse());

        mockMvc.perform(patch(APPLICATION_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteUser - should return 403 Forbidden when called by recruiter")
    @WithMockUser(username = "recruiter@candidai.com", roles = "RECRUITER")
    void givenRecruiterRole_deleteUser_shouldReturnForbidden() throws Exception {
        mockMvc.perform(delete(USER_DELETE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteUser - should return 204 NoContent when called by admin")
    @WithMockUser(username = "admin@candidai.com", roles = "ADMIN")
    void givenAdminRole_deleteUser_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(any(UUID.class));

        mockMvc.perform(delete(USER_DELETE_URL))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("cancelInterview - should return 403 Forbidden when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_cancelInterview_shouldReturnForbidden() throws Exception {
        mockMvc.perform(patch(INTERVIEW_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Candidate withdrew\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cancelInterview - should return 200 Ok when called by tech lead")
    @WithMockUser(username = "techlead@candidai.com", roles = "TECH_LEAD")
    void givenTechLeadRole_cancelInterview_shouldReturnOk() throws Exception {
        InterviewResponse mockInterview = new InterviewResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Alex Smith",
                ua.edu.ukma.candidai.interview.model.InterviewType.TECHNICAL,
                InterviewStatus.CANCELLED,
                Instant.now(),
                60,
                "https://meet.candidai.com/abc",
                "Notes",
                "Cancelled by tech lead",
                Instant.now(),
                Instant.now()
        );
        when(interviewService.cancelInterview(any(UUID.class), any(CancelInterviewRequest.class)))
                .thenReturn(mockInterview);

        mockMvc.perform(patch(INTERVIEW_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancelled by tech lead\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("triggerScreening - should return 403 Forbidden when called by candidate")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void givenCandidateRole_triggerScreening_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post(ASSESSMENT_SCREENING_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("triggerScreening - should return 200 Ok when called by recruiter")
    @WithMockUser(username = "recruiter@candidai.com", roles = "RECRUITER")
    void givenRecruiterRole_triggerScreening_shouldReturnOk() throws Exception {
        AiScreeningResult screeningResult = AiScreeningResult.completed(
                UUID.randomUUID(),
                UUID.randomUUID(),
                88,
                true,
                "Recommended for technical interview",
                List.of("Strong Java and Spring knowledge"),
                List.of("Lacks Docker production experience"),
                List.of("Explain Spring Security architecture"),
                Instant.now()
        );
        when(assessmentService.executeScreening(any(UUID.class)))
                .thenReturn(screeningResult);

        mockMvc.perform(post(ASSESSMENT_SCREENING_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteCompany - should return 403 Forbidden for anonymous user")
    @WithAnonymousUser
    void givenAnonymousUser_deleteCompany_shouldDenyAccess() throws Exception {
        mockMvc.perform(delete(COMPANY_DELETE_URL))
                .andExpect(status().isForbidden());
    }
}
