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
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
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

        Method overloadedUpdateStatusMethod = implClass.getDeclaredMethod(
                "updateStatus", UUID.class,
                ua.edu.ukma.candidai.recruitment.dto.model.ApplicationStatus.class,
                Integer.class, String.class
        );
        AuditAction overloadedUpdateAudit = overloadedUpdateStatusMethod.getAnnotation(AuditAction.class);
        assertThat(overloadedUpdateAudit).isNotNull();
        assertThat(overloadedUpdateAudit.action()).isEqualTo("UPDATE_APPLICATION_STATUS");

        Method delegateEvaluateMethod = implClass.getDeclaredMethod("evaluateCandidate", UUID.class);
        assertThat(delegateEvaluateMethod.getAnnotation(AuditAction.class)).isNull();

        Class<?> evaluationClass = Class.forName(
                "ua.edu.ukma.candidai.recruitment.service.CandidateEvaluationServiceImpl"
        );
        Method evaluateMethod = evaluationClass.getDeclaredMethod("evaluateCandidate", UUID.class);
        AuditAction evaluateAudit = evaluateMethod.getAnnotation(AuditAction.class);
        assertThat(evaluateAudit).isNotNull();
        assertThat(evaluateAudit.action()).isEqualTo("EVALUATE_APPLICATION");
    }

    @Test
    @DisplayName("UserServiceImpl and AuthController should declare @AuditAction on user and login methods")
    void givenUserServiceImplAndAuthController_whenCheckingMethods_thenAuditActionPresent() throws Exception {
        Class<?> implClass = Class.forName("ua.edu.ukma.candidai.user.service.UserServiceImpl");

        Method createMethod = implClass.getDeclaredMethod("createUser", CreateUserRequest.class);
        AuditAction createAudit = createMethod.getAnnotation(AuditAction.class);
        assertThat(createAudit).isNotNull();
        assertThat(createAudit.action()).isEqualTo("CREATE_USER");
        assertThat(createAudit.description()).isNotEmpty();

        Class<?> authControllerClass = Class.forName(
                "ua.edu.ukma.candidai.common.security.controller.AuthController"
        );
        Method loginMethod = authControllerClass.getDeclaredMethod("login", LoginRequest.class);
        AuditAction loginAudit = loginMethod.getAnnotation(AuditAction.class);
        assertThat(loginAudit).isNotNull();
        assertThat(loginAudit.action()).isEqualTo("USER_LOGIN");
        assertThat(loginAudit.description()).isNotEmpty();
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

        RecordComponent loginComponent = Arrays.stream(LoginRequest.class.getRecordComponents())
                .filter(rc -> rc.getName().equals("password"))
                .findFirst()
                .orElseThrow();
        AuditSensitive componentSensitive = loginComponent.getAnnotation(AuditSensitive.class);
        assertThat(componentSensitive).isNotNull();
        assertThat(componentSensitive.mask()).isEqualTo("***MASKED***");
    }
}
