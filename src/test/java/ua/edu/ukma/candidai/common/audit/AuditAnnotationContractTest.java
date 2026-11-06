package ua.edu.ukma.candidai.common.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.candidai.common.audit.annotation.AuditAction;
import ua.edu.ukma.candidai.common.audit.annotation.AuditSensitive;
import ua.edu.ukma.candidai.common.security.dto.LoginRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.ApplyForVacancyRequest;
import ua.edu.ukma.candidai.recruitment.dto.request.UpdateApplicationStatusRequest;
import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit contract tests for @AuditAction and @AuditSensitive annotations")
class AuditAnnotationContractTest {

    @Test
    @DisplayName("VacancyServiceImpl should declare @AuditAction on core business lifecycle methods")
    void givenVacancyServiceImpl_whenCheckingMethods_thenAuditActionPresent() throws Exception {
        Class<?> implClass = Class.forName("ua.edu.ukma.candidai.vacancy.service.VacancyServiceImpl");

        Method createMethod = implClass.getDeclaredMethod("createVacancy", CreateVacancyRequest.class);
        AuditAction createAudit = createMethod.getAnnotation(AuditAction.class);
        assertThat(createAudit).isNotNull();
        assertThat(createAudit.action()).isEqualTo("CREATE_VACANCY");
        assertThat(createAudit.description()).isNotEmpty();
        assertThat(createAudit.logParameters()).isTrue();

        Method updateStatusMethod = implClass.getDeclaredMethod(
                "updateVacancyStatus", UUID.class, UpdateVacancyStatusRequest.class
        );
        AuditAction updateAudit = updateStatusMethod.getAnnotation(AuditAction.class);
        assertThat(updateAudit).isNotNull();
        assertThat(updateAudit.action()).isEqualTo("UPDATE_VACANCY_STATUS");
        assertThat(updateAudit.description()).isNotEmpty();

        Method deleteMethod = implClass.getDeclaredMethod("deleteVacancy", UUID.class);
        AuditAction deleteAudit = deleteMethod.getAnnotation(AuditAction.class);
        assertThat(deleteAudit).isNotNull();
        assertThat(deleteAudit.action()).isEqualTo("DELETE_VACANCY");
        assertThat(deleteAudit.description()).isNotEmpty();
    }

    @Test
    @DisplayName("ApplicationServiceImpl should declare @AuditAction on application lifecycle and evaluation methods")
    void givenApplicationServiceImpl_whenCheckingMethods_thenAuditActionPresent() throws Exception {
        Class<?> implClass = Class.forName("ua.edu.ukma.candidai.recruitment.service.ApplicationServiceImpl");

        Method applyMethod = implClass.getDeclaredMethod("apply", ApplyForVacancyRequest.class);
        AuditAction applyAudit = applyMethod.getAnnotation(AuditAction.class);
        assertThat(applyAudit).isNotNull();
        assertThat(applyAudit.action()).isEqualTo("APPLY_VACANCY");
        assertThat(applyAudit.description()).isNotEmpty();

        Method updateStatusMethod = implClass.getDeclaredMethod(
                "updateStatus", UUID.class, UpdateApplicationStatusRequest.class
        );
        AuditAction updateAudit = updateStatusMethod.getAnnotation(AuditAction.class);
        assertThat(updateAudit).isNotNull();
        assertThat(updateAudit.action()).isEqualTo("UPDATE_APPLICATION_STATUS");

        Method evaluateMethod = implClass.getDeclaredMethod("evaluateCandidate", UUID.class);
        AuditAction evaluateAudit = evaluateMethod.getAnnotation(AuditAction.class);
        assertThat(evaluateAudit).isNotNull();
        assertThat(evaluateAudit.action()).isEqualTo("EVALUATE_APPLICATION");
    }

    @Test
    @DisplayName("UserServiceImpl should declare @AuditAction on user creation method")
    void givenUserServiceImpl_whenCheckingCreateUser_thenAuditActionPresent() throws Exception {
        Class<?> implClass = Class.forName("ua.edu.ukma.candidai.user.service.UserServiceImpl");

        Method createMethod = implClass.getDeclaredMethod("createUser", CreateUserRequest.class);
        AuditAction createAudit = createMethod.getAnnotation(AuditAction.class);
        assertThat(createAudit).isNotNull();
        assertThat(createAudit.action()).isEqualTo("CREATE_USER");
        assertThat(createAudit.description()).isNotEmpty();
    }

    @Test
    @DisplayName("LoginRequest and CreateUserRequest should mark password fields with @AuditSensitive")
    void givenAuthenticationDtos_whenCheckingPasswordFields_thenAuditSensitivePresent() throws Exception {
        Field loginPassword = LoginRequest.class.getDeclaredField("password");
        AuditSensitive loginSensitive = loginPassword.getAnnotation(AuditSensitive.class);
        assertThat(loginSensitive).isNotNull();
        assertThat(loginSensitive.mask()).isEqualTo("***MASKED***");

        Field createUserPassword = CreateUserRequest.class.getDeclaredField("password");
        AuditSensitive userSensitive = createUserPassword.getAnnotation(AuditSensitive.class);
        assertThat(userSensitive).isNotNull();
        assertThat(userSensitive.mask()).isEqualTo("***MASKED***");
    }
}
