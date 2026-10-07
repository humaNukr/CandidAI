package ua.edu.ukma.candidai.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class OpenApiDocumentationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("apiDocs - should return valid OpenAPI 3.1 documentation with title, paths, and security")
    void givenSpringDocEnabled_getApiDocs_shouldReturnOpenApiSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(containsString("3.")))
                .andExpect(jsonPath("$.info.title").value("CandidAI API"))
                .andExpect(jsonPath("$.info.version").value("v1.0"))
                .andExpect(jsonPath("$.paths['/api/v1/vacancies']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/applications']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/applications/{id}/screening']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/interviews']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/companies']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/resumes/upload']").exists())
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Auth']").exists());
    }

    @Test
    @DisplayName("swaggerUi - should provide Swagger UI html redirect or page")
    void givenSpringDocEnabled_getSwaggerUi_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
