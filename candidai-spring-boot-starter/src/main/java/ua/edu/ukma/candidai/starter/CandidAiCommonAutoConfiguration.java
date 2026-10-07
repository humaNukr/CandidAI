package ua.edu.ukma.candidai.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ua.edu.ukma.candidai.starter.filter.CommonResponseMetadataFilter;
import ua.edu.ukma.candidai.starter.properties.CandidAiStarterProperties;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(CandidAiStarterProperties.class)
@ConditionalOnProperty(prefix = "candidai.starter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CandidAiCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommonResponseMetadataFilter commonResponseMetadataFilter(CandidAiStarterProperties properties) {
        return new CommonResponseMetadataFilter(properties);
    }
}
