package ua.edu.ukma.candidai.starter.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "candidai.starter")
@Validated
public record CandidAiStarterProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("development") @NotBlank String environment,
        @Valid @DefaultValue ResponseProperties response
) {

    public record ResponseProperties(
            @DefaultValue("true") boolean includeExecutionTime,
            @DefaultValue("true") boolean includeEnvironment,
            @DefaultValue("CandidAI") @NotBlank String serviceName
    ) {}
}
