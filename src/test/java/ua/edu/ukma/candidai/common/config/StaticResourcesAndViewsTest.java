package ua.edu.ukma.candidai.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.vacancy.VacancyApi;
import ua.edu.ukma.candidai.vacancy.VacancyDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statictestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Import(TestWebSecurityConfiguration.class)
class StaticResourcesAndViewsTest {

    private static final String CSS_PATH = "/css/app.css";
    private static final String CACHE_MAX_AGE = "max-age=31536000";
    private static final UUID VACANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VacancyApi vacancyApi;

    @MockitoBean
    private UserApi userApi;

    @Test
    @DisplayName("CSS static resource has one-year public cache header")
    void css_shouldBeCachedForOneYear() throws Exception {
        mockMvc.perform(get(CSS_PATH))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString(CACHE_MAX_AGE)));
    }

    @Test
    @DisplayName("CSS resource with gzip accept encoding returns pre-compressed file")
    void css_withGzipAcceptEncoding_shouldReturnPrecompressedFile() throws Exception {
        mockMvc.perform(get(CSS_PATH).header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_ENCODING, "gzip"));
    }

    @Test
    @DisplayName("Apply page includes content-versioned CSS link and CSRF meta tags")
    @WithMockUser(username = "candidate@candidai.com", roles = "CANDIDATE")
    void applyPage_shouldReferenceVersionedCssAndExposeCsrfMeta() throws Exception {
        when(vacancyApi.getVacancyDetails(VACANCY_ID)).thenReturn(
                new VacancyDetails(VACANCY_ID, "Java Developer", "Description", List.of(), List.of(), "Senior")
        );
        when(userApi.getUserNotificationProfileByEmail("candidate@candidai.com")).thenReturn(
                Optional.of(new UserNotificationProfile(UUID.randomUUID(), "Jane Doe", "candidate@candidai.com", null))
        );

        mockMvc.perform(get("/vacancies/" + VACANCY_ID + "/apply")
                        .with(user("candidate@candidai.com").roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*/css/app-[0-9a-f]{32}\\.css.*")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"_csrf_header\"")));
    }

    @Test
    @DisplayName("REST API response includes ETag header and supports 304 Not Modified")
    void apiEndpoint_shouldIncludeEtagAndSupport304() throws Exception {
        String etag = mockMvc.perform(get("/api/v1/vacancies"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/api/v1/vacancies")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }
}
