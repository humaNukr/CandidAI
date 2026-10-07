package ua.edu.ukma.candidai.starter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.edu.ukma.candidai.starter.filter.CommonResponseMetadataFilter;
import ua.edu.ukma.candidai.starter.properties.CandidAiStarterProperties;

public final class CandidAiCommonAutoConfigurationTestResources {

    public static final String STARTER_DISABLED_PROPERTY = "candidai.starter.enabled=false";
    public static final String CUSTOM_ENVIRONMENT_PROPERTY = "candidai.starter.environment=production";
    public static final String CUSTOM_SERVICE_NAME_PROPERTY = "candidai.starter.response.service-name=CustomApp";

    public static final CandidAiStarterProperties EXPECTED_DEFAULT_PROPERTIES = new CandidAiStarterProperties(
            true,
            "development",
            new CandidAiStarterProperties.ResponseProperties(true, true, "CandidAI")
    );

    public static final CandidAiStarterProperties EXPECTED_CUSTOM_PROPERTIES = new CandidAiStarterProperties(
            true,
            "production",
            new CandidAiStarterProperties.ResponseProperties(true, true, "CustomApp")
    );

    public static final CommonResponseMetadataFilter CUSTOM_FILTER = new CommonResponseMetadataFilter(
            new CandidAiStarterProperties(
                    true,
                    "custom-env",
                    new CandidAiStarterProperties.ResponseProperties(false, false, "CustomService")
            )
    );

    private CandidAiCommonAutoConfigurationTestResources() {
    }

    @Configuration(proxyBeanMethods = false)
    public static class CustomFilterConfiguration {

        @Bean
        public CommonResponseMetadataFilter customFilter() {
            return CUSTOM_FILTER;
        }
    }
}
