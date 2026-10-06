package ua.edu.ukma.candidai.starter.filter;

import ua.edu.ukma.candidai.starter.properties.CandidAiStarterProperties;

public final class CommonResponseMetadataFilterTestResources {

    public static final String TEST_SERVICE_NAME = "TestApp";
    public static final String TEST_ENVIRONMENT = "staging";
    public static final String HEADER_APPLICATION_NAME = "X-Application-Name";
    public static final String HEADER_ENVIRONMENT = "X-Environment";
    public static final String HEADER_RESPONSE_TIME = "X-Response-Time-Millis";

    private CommonResponseMetadataFilterTestResources() {
    }

    public static CandidAiStarterProperties createEnabledProperties() {
        return new CandidAiStarterProperties(
                true,
                TEST_ENVIRONMENT,
                new CandidAiStarterProperties.ResponseProperties(true, true, TEST_SERVICE_NAME)
        );
    }

    public static CandidAiStarterProperties createExecutionTimeDisabledProperties() {
        return new CandidAiStarterProperties(
                true,
                TEST_ENVIRONMENT,
                new CandidAiStarterProperties.ResponseProperties(false, true, TEST_SERVICE_NAME)
        );
    }

    public static CandidAiStarterProperties createEnvironmentDisabledProperties() {
        return new CandidAiStarterProperties(
                true,
                TEST_ENVIRONMENT,
                new CandidAiStarterProperties.ResponseProperties(true, false, TEST_SERVICE_NAME)
        );
    }
}
