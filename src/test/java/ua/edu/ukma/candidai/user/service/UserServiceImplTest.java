package ua.edu.ukma.candidai.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.user.UserRole;
import ua.edu.ukma.candidai.user.dto.request.CreateUserRequest;
import ua.edu.ukma.candidai.user.dto.request.UpdateUserRequest;
import ua.edu.ukma.candidai.user.dto.response.UserResponse;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_EMAIL;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.NON_EXISTENT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.UPDATED_FULL_NAME;
import static ua.edu.ukma.candidai.user.UserTestResources.UPDATED_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.expectedDuplicateEmailMessage;
import static ua.edu.ukma.candidai.user.UserTestResources.expectedUserNotFoundMessage;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleCreateUserRequest;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUpdateUserRequest;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUser;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CommonGenerator commonGenerator;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("createUser should save user and return response when email does not exist")
    void givenValidRequest_createUser_shouldSaveUserAndReturnResponse() {
        CreateUserRequest request = sampleCreateUserRequest();
        User user = sampleUser();
        UserResponse expectedResponse = sampleUserResponse();

        when(userRepository.existsByEmail(DEFAULT_EMAIL)).thenReturn(false);
        when(commonGenerator.uuid()).thenReturn(DEFAULT_USER_ID);
        when(commonGenerator.now()).thenReturn(DEFAULT_NOW);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.createUser(request);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("createUser should throw DuplicateResourceException when email already exists")
    void givenExistingEmail_createUser_shouldThrowDuplicateResourceException() {
        CreateUserRequest request = sampleCreateUserRequest();

        when(userRepository.existsByEmail(DEFAULT_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage(expectedDuplicateEmailMessage(DEFAULT_EMAIL));
    }

    @Test
    @DisplayName("getUserById should return user response when user exists")
    void givenExistingId_getUserById_shouldReturnUserResponse() {
        User user = sampleUser();
        UserResponse expectedResponse = sampleUserResponse();

        when(userRepository.findById(DEFAULT_USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.getUserById(DEFAULT_USER_ID);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("getUserById should throw ResourceNotFoundException when user does not exist")
    void givenNonExistentId_getUserById_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(NON_EXISTENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedUserNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    @DisplayName("getAllUsers should return users filtered by role when role is provided")
    void givenRole_getAllUsers_shouldReturnUsersFilteredByRole() {
        User user = sampleUser();
        UserResponse expectedResponse = sampleUserResponse();

        when(userRepository.findByRole(UserRole.RECRUITER)).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        List<UserResponse> responses = userService.getAllUsers(UserRole.RECRUITER);

        assertThat(responses).containsExactly(expectedResponse);
    }

    @Test
    @DisplayName("getAllUsers should return all users when role is null")
    void givenNullRole_getAllUsers_shouldReturnAllUsers() {
        User user = sampleUser();
        UserResponse expectedResponse = sampleUserResponse();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        List<UserResponse> responses = userService.getAllUsers(null);

        assertThat(responses).containsExactly(expectedResponse);
    }

    @Test
    @DisplayName("updateUser should update fields, save, and return response when user exists")
    void givenExistingId_updateUser_shouldUpdateFieldsSaveAndReturnResponse() {
        User user = sampleUser();
        UpdateUserRequest request = sampleUpdateUserRequest();
        UserResponse expectedResponse = new UserResponse(
                DEFAULT_USER_ID,
                UPDATED_FULL_NAME,
                DEFAULT_EMAIL,
                UserRole.RECRUITER,
                UPDATED_TELEGRAM_CHAT_ID,
                user.getCreatedAt()
        );

        when(userRepository.findById(DEFAULT_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.updateUser(DEFAULT_USER_ID, request);

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(user.getFullName()).isEqualTo(UPDATED_FULL_NAME);
        assertThat(user.getTelegramChatId()).isEqualTo(UPDATED_TELEGRAM_CHAT_ID);
    }

    @Test
    @DisplayName("updateUser should only update non-null fields from request")
    void givenPartialUpdate_updateUser_shouldOnlyUpdateProvidedFields() {
        User user = sampleUser();
        UpdateUserRequest request = new UpdateUserRequest(UPDATED_FULL_NAME, null);
        UserResponse expectedResponse = new UserResponse(
                DEFAULT_USER_ID,
                UPDATED_FULL_NAME,
                DEFAULT_EMAIL,
                UserRole.RECRUITER,
                DEFAULT_TELEGRAM_CHAT_ID,
                user.getCreatedAt()
        );

        when(userRepository.findById(DEFAULT_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.updateUser(DEFAULT_USER_ID, request);

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(user.getFullName()).isEqualTo(UPDATED_FULL_NAME);
        assertThat(user.getTelegramChatId()).isEqualTo(DEFAULT_TELEGRAM_CHAT_ID);
    }

    @Test
    @DisplayName("updateUser should throw ResourceNotFoundException when user does not exist")
    void givenNonExistentId_updateUser_shouldThrowResourceNotFoundException() {
        UpdateUserRequest request = sampleUpdateUserRequest();

        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(NON_EXISTENT_USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedUserNotFoundMessage(NON_EXISTENT_USER_ID));
    }

    @Test
    @DisplayName("deleteUser should find user and delete by id when user exists")
    void givenExistingId_deleteUser_shouldDeleteById() {
        User user = sampleUser();

        when(userRepository.findById(DEFAULT_USER_ID)).thenReturn(Optional.of(user));

        userService.deleteUser(DEFAULT_USER_ID);

        verify(userRepository).deleteById(DEFAULT_USER_ID);
    }

    @Test
    @DisplayName("deleteUser should throw ResourceNotFoundException when user does not exist")
    void givenNonExistentId_deleteUser_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(NON_EXISTENT_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedUserNotFoundMessage(NON_EXISTENT_USER_ID));
    }
}
