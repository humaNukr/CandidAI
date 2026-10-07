package ua.edu.ukma.candidai.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CandidAI API",
                version = "v1.0",
                description = "REST API documentation for CandidAI - Smart HR and AI Screening Platform",
                contact = @Contact(
                        name = "CandidAI Engineering Team",
                        email = "support@candidai.ukma.edu.ua",
                        url = "https://github.com/humaNukr/CandidAI"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server"),
                @Server(url = "https://api.candidai.ukma.edu.ua", description = "Production Server")
        },
        tags = {
                @Tag(name = "Vacancies", description = "Operations related to job vacancy management"),
                @Tag(name = "Applications", description = "Candidate application submission and lifecycle management"),
                @Tag(name = "Assessments", description = "AI Candidate resume screening and assessment operations"),
                @Tag(name = "Interviews", description = "Interview scheduling and candidate feedback coordination"),
                @Tag(name = "Users", description = "User, recruiter, and candidate account management")
        },
        security = {
                @SecurityRequirement(name = "Bearer Auth")
        }
)
@SecurityScheme(
        name = "Bearer Auth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token authorization header"
)
public class OpenApiConfiguration {
}
