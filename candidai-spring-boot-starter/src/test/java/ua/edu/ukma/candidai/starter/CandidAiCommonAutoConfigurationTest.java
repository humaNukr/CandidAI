package ua.edu.ukma.candidai.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import ua.edu.ukma.candidai.starter.filter.CommonResponseMetadataFilter;
import ua.edu.ukma.candidai.starter.properties.CandidAiStarterProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ua.edu.ukma.candidai.starter.CandidAiCommonAutoConfigurationTestResources.*;

class CandidAiCommonAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CandidAiCommonAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CandidAiCommonAutoConfiguration.class));

    @Test
    @DisplayName("load - should register starter properties and filter beans with defaults")
    void givenDefaultConfiguration_load_shouldRegisterStarterBeans() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(CandidAiStarterProperties.class);
            assertThat(context).hasSingleBean(CommonResponseMetadataFilter.class);

            CandidAiStarterProperties actualProperties = context.getBean(CandidAiStarterProperties.class);

            assertThat(actualProperties).usingRecursiveComparison().isEqualTo(EXPECTED_DEFAULT_PROPERTIES);
        });
    }

    @Test
    @DisplayName("load - should not register filter when starter is explicitly disabled")
    void givenStarterDisabled_load_shouldNotRegisterStarterBeans() {
        webContextRunner.withPropertyValues(STARTER_DISABLED_PROPERTY)
                .run(context -> assertThat(context).doesNotHaveBean(CommonResponseMetadataFilter.class));
    }

    @Test
    @DisplayName("load - should back off when custom CommonResponseMetadataFilter bean is provided")
    void givenCustomBeanProvided_load_shouldBackOff() {
        webContextRunner.withUserConfiguration(CustomFilterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CommonResponseMetadataFilter.class);
                    assertThat(context).hasBean("customFilter");
                    assertThat(context).doesNotHaveBean("commonResponseMetadataFilter");

                    CommonResponseMetadataFilter filter = context.getBean(CommonResponseMetadataFilter.class);

                    assertThat(filter).isSameAs(CUSTOM_FILTER);
                });
    }

    @Test
    @DisplayName("load - should bind custom properties correctly")
    void givenCustomProperties_load_shouldBindProperties() {
        webContextRunner.withPropertyValues(CUSTOM_ENVIRONMENT_PROPERTY, CUSTOM_SERVICE_NAME_PROPERTY)
                .run(context -> {
                    assertThat(context).hasSingleBean(CandidAiStarterProperties.class);

                    CandidAiStarterProperties actualProperties = context.getBean(CandidAiStarterProperties.class);

                    assertThat(actualProperties).usingRecursiveComparison().isEqualTo(EXPECTED_CUSTOM_PROPERTIES);
                });
    }

    @Test
    @DisplayName("load - should not register filter in non-web application context")
    void givenNonWebApplication_load_shouldNotRegisterFilter() {
        nonWebContextRunner.run(context -> assertThat(context).doesNotHaveBean(CommonResponseMetadataFilter.class));
    }

    @Test
    @DisplayName("load - should discover auto-configuration through Spring Boot imports file")
    void givenAutoConfigurationImports_load_shouldContainStarterAutoConfiguration() {
        List<String> configurations = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertThat(configurations).contains(CandidAiCommonAutoConfiguration.class.getName());
    }
}
