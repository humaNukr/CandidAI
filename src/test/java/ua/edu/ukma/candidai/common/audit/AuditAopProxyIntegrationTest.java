package ua.edu.ukma.candidai.common.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.audit.annotation.AuditAction;
import ua.edu.ukma.candidai.recruitment.service.ApplicationService;
import ua.edu.ukma.candidai.recruitment.service.CandidateEvaluationService;
import ua.edu.ukma.candidai.user.service.UserService;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.service.VacancyService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ua.edu.ukma.candidai.testutils.IntegrationTestResources.aValidCreateVacancyRequest;

@IntegrationTest
@Import(AuditAopProxyIntegrationTest.TestAuditAspectConfiguration.class)
@DisplayName("Integration tests for Spring AOP CGLIB proxies and @AuditAction advice interception")
class AuditAopProxyIntegrationTest {

    @Autowired
    private VacancyService vacancyService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private CandidateEvaluationService candidateEvaluationService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestAuditAspect testAuditAspect;

    @Test
    @DisplayName("Services annotated with @AuditAction must be proxied by Spring AOP CGLIB proxy")
    void givenAnnotatedServices_whenInspectingBeans_thenAllAreAopCglibProxies() {
        assertThat(AopUtils.isAopProxy(vacancyService)).isTrue();
        assertThat(AopUtils.isCglibProxy(vacancyService)).isTrue();

        assertThat(AopUtils.isAopProxy(applicationService)).isTrue();
        assertThat(AopUtils.isCglibProxy(applicationService)).isTrue();

        assertThat(AopUtils.isAopProxy(candidateEvaluationService)).isTrue();
        assertThat(AopUtils.isCglibProxy(candidateEvaluationService)).isTrue();

        assertThat(AopUtils.isAopProxy(userService)).isTrue();
        assertThat(AopUtils.isCglibProxy(userService)).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("Calling @AuditAction annotated service method must trigger AOP advice interception")
    void givenAuditActionMethod_whenInvoked_thenAopAdviceInterceptsInvocation() {
        testAuditAspect.clearInterceptedActions();

        CreateVacancyRequest request = aValidCreateVacancyRequest();

        VacancyResponse response = vacancyService.createVacancy(request);

        assertThat(response).isNotNull();
        assertThat(testAuditAspect.getInterceptedActions()).contains("CREATE_VACANCY");
    }

    @TestConfiguration
    static class TestAuditAspectConfiguration {

        @Bean
        public TestAuditAspect testAuditAspect() {
            return new TestAuditAspect();
        }
    }

    @Aspect
    public static class TestAuditAspect {

        private final List<String> interceptedActions = Collections.synchronizedList(new ArrayList<>());

        @Around("@annotation(auditAction)")
        public Object intercept(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
            interceptedActions.add(auditAction.action());
            return joinPoint.proceed();
        }

        public List<String> getInterceptedActions() {
            return new ArrayList<>(interceptedActions);
        }

        public void clearInterceptedActions() {
            interceptedActions.clear();
        }
    }
}
