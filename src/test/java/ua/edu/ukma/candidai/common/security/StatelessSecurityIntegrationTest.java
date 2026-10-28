package ua.edu.ukma.candidai.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.annotation.IntegrationTest;
import ua.edu.ukma.candidai.common.security.dto.AuthResponse;
import ua.edu.ukma.candidai.common.security.dto.LoginRequest;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.ukma.candidai.common.security.SecurityTestResources.*;

@IntegrationTest
class StatelessSecurityIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PROTECTED_USERS_URL = "/api/v1/users";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Accessing protected resource without token should return 401 ProblemDetail")
    void givenNoToken_accessProtectedResource_shouldReturn401ProblemDetail() throws Exception {
        mockMvc.perform(get(PROTECTED_USERS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Автентифікація обов'язкова"))
                .andExpect(jsonPath("$.path").value(PROTECTED_USERS_URL));
    }

    @Test
    @DisplayName("Valid login request should return 200 OK with JWT AuthResponse")
    void givenValidCredentials_login_shouldReturn200WithJwtToken() throws Exception {
        LoginRequest request = aValidLoginRequest();

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.tokenType()).isEqualTo("Bearer");
        assertThat(authResponse.expiresIn()).isPositive();
    }

    @Test
    @DisplayName("Invalid login request should return 401 Unauthorized ProblemDetail")
    void givenInvalidCredentials_login_shouldReturn401Unauthorized() throws Exception {
        LoginRequest request = anInvalidLoginRequest();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Автентифікація обов'язкова"));
    }

    @Test
    @DisplayName("Accessing protected resource with valid Bearer token should succeed")
    void givenValidBearerToken_accessProtectedResource_shouldReturn200Ok() throws Exception {
        LoginRequest request = anAdminLoginRequest();

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertThat(authResponse.accessToken()).isNotBlank();

        mockMvc.perform(get(PROTECTED_USERS_URL)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + authResponse.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Accessing OpenAPI docs without authentication should return 200 OK")
    void givenAnonymousUser_accessOpenApiDocs_shouldReturn200Ok() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
