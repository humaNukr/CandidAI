package ua.edu.ukma.candidai.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.user.dto.request.UpdateUserRequest;
import ua.edu.ukma.candidai.user.dto.response.UserResponse;
import ua.edu.ukma.candidai.user.service.UserService;

import java.net.URI;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.ukma.candidai.user.UserTestResources.BASE_URL;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_PASSWORD;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_ROLE;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.NON_EXISTENT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.SECOND_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.UPDATED_FULL_NAME;
import static ua.edu.ukma.candidai.user.UserTestResources.UPDATED_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.expectedDuplicateEmailMessage;
import static ua.edu.ukma.candidai.user.UserTestResources.expectedUserNotFoundMessage;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleCreateUserRequest;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUpdateUserRequest;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUserResponse;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/v1/users - should create user and return 201 Created")
    void givenValidRequest_createUser_shouldReturn201Created() throws Exception {
        CreateUserRequest request = sampleCreateUserRequest();
        UserResponse expectedResponse = sampleUserResponse();

        when(userService.createUser(request)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse actual = parseResponse(result, UserResponse.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 400 ProblemDetail when email is invalid")
    void givenInvalidEmail_createUser_shouldReturn400BadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "John Doe",
                "invalid-email",
                DEFAULT_PASSWORD,
                DEFAULT_ROLE,
                DEFAULT_TELEGRAM_CHAT_ID
        );

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://candidai.ukma.edu.ua/errors/validation"));
        assertThat(extractErrors(problemDetail)).containsKey("email");
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 400 ProblemDetail when password is too short")
    void givenShortPassword_createUser_shouldReturn400BadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "John Doe",
                DEFAULT_EMAIL,
                "short",
                DEFAULT_ROLE,
                DEFAULT_TELEGRAM_CHAT_ID
        );

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(extractErrors(problemDetail)).containsKey("password");
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 400 ProblemDetail when fullName is blank")
    void givenBlankFullName_createUser_shouldReturn400BadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "   ",
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                DEFAULT_ROLE,
                DEFAULT_TELEGRAM_CHAT_ID
        );

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(extractErrors(problemDetail)).containsKey("fullName");
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 400 ProblemDetail when role is null")
    void givenNullRole_createUser_shouldReturn400BadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "John Doe",
                DEFAULT_EMAIL,
                DEFAULT_PASSWORD,
                null,
                DEFAULT_TELEGRAM_CHAT_ID
        );

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(extractErrors(problemDetail)).containsKey("role");
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 409 Conflict when email already exists")
    void givenDuplicateEmail_createUser_shouldReturn409Conflict() throws Exception {
        CreateUserRequest request = sampleCreateUserRequest();
        String message = expectedDuplicateEmailMessage(DEFAULT_EMAIL);

        when(userService.createUser(request))
                .thenThrow(new DuplicateResourceException(message));

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Conflict");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - should return 200 Ok when user exists")
    void givenExistingId_getUserById_shouldReturn200Ok() throws Exception {
        UserResponse expectedResponse = sampleUserResponse();

        when(userService.getUserById(DEFAULT_USER_ID)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + DEFAULT_USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse actual = parseResponse(result, UserResponse.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - should return 404 ProblemDetail when user not found")
    void givenNonExistentId_getUserById_shouldReturn404NotFound() throws Exception {
        String message = expectedUserNotFoundMessage(NON_EXISTENT_USER_ID);

        when(userService.getUserById(NON_EXISTENT_USER_ID))
                .thenThrow(new ResourceNotFoundException(message));

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + NON_EXISTENT_USER_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    @Test
    @DisplayName("GET /api/v1/users - should return 200 Ok with filtered list when role is provided")
    void givenRoleParam_getAllUsers_shouldReturn200OkWithFilteredUsers() throws Exception {
        UserResponse recruiter = sampleUserResponse();

        when(userService.getAllUsers(UserRole.RECRUITER)).thenReturn(List.of(recruiter));

        MvcResult result = mockMvc.perform(get(BASE_URL).param("role", "RECRUITER"))
                .andExpect(status().isOk())
                .andReturn();

        List<UserResponse> actual = parseResponseList(result, UserResponse.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(recruiter));
    }

    @Test
    @DisplayName("GET /api/v1/users - should return 200 Ok with all users when no role param is provided")
    void givenNoRoleParam_getAllUsers_shouldReturn200OkWithAllUsers() throws Exception {
        UserResponse user1 = sampleUserResponse();
        UserResponse user2 = new UserResponse(
                SECOND_USER_ID,
                "Candidate One",
                "cand@example.com",
                UserRole.CANDIDATE,
                null,
                DEFAULT_NOW
        );

        when(userService.getAllUsers(null)).thenReturn(List.of(user1, user2));

        MvcResult result = mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andReturn();

        List<UserResponse> actual = parseResponseList(result, UserResponse.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(List.of(user1, user2));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id} - should return 200 Ok and updated user")
    void givenValidUpdateRequest_updateUser_shouldReturn200Ok() throws Exception {
        UpdateUserRequest request = sampleUpdateUserRequest();
        UserResponse expectedResponse = new UserResponse(
                DEFAULT_USER_ID,
                UPDATED_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_ROLE,
                UPDATED_TELEGRAM_CHAT_ID,
                DEFAULT_NOW
        );

        when(userService.updateUser(DEFAULT_USER_ID, request)).thenReturn(expectedResponse);

        MvcResult result = mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse actual = parseResponse(result, UserResponse.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id} - should return 400 ProblemDetail when fullName exceeds 100 characters")
    void givenInvalidUpdateRequest_updateUser_shouldReturn400BadRequest() throws Exception {
        UpdateUserRequest invalidRequest = new UpdateUserRequest("A".repeat(101), null);

        MvcResult result = mockMvc.perform(patch(BASE_URL + "/" + DEFAULT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation Error");
        assertThat(extractErrors(problemDetail)).containsKey("fullName");
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id} - should return 404 ProblemDetail when user not found")
    void givenNonExistentId_updateUser_shouldReturn404NotFound() throws Exception {
        UpdateUserRequest request = sampleUpdateUserRequest();
        String message = expectedUserNotFoundMessage(NON_EXISTENT_USER_ID);

        when(userService.updateUser(NON_EXISTENT_USER_ID, request))
                .thenThrow(new ResourceNotFoundException(message));

        MvcResult result = mockMvc.perform(patch(BASE_URL + "/" + NON_EXISTENT_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - should return 204 No Content when user deleted")
    void givenExistingId_deleteUser_shouldReturn204NoContent() throws Exception {
        doNothing().when(userService).deleteUser(DEFAULT_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/" + DEFAULT_USER_ID))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(DEFAULT_USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - should return 404 ProblemDetail when user not found")
    void givenNonExistentId_deleteUser_shouldReturn404NotFound() throws Exception {
        String message = expectedUserNotFoundMessage(NON_EXISTENT_USER_ID);

        doThrow(new ResourceNotFoundException(message)).when(userService).deleteUser(NON_EXISTENT_USER_ID);

        MvcResult result = mockMvc.perform(delete(BASE_URL + "/" + NON_EXISTENT_USER_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail problemDetail = parseProblemDetail(result);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problemDetail.getDetail()).isEqualTo(message);
    }

    private <T> T parseResponse(MvcResult result, Class<T> clazz) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), clazz);
    }

    private <T> List<T> parseResponseList(MvcResult result, Class<T> elementType) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
        );
    }

    private ProblemDetail parseProblemDetail(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractErrors(ProblemDetail problemDetail) {
        return (Map<String, String>) problemDetail.getProperties().get("errors");
    }
}
