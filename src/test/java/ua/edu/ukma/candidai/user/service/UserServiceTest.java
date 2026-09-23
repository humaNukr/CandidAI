package ua.edu.ukma.candidai.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.NON_EXISTENT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.UPDATED_TELEGRAM_CHAT_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.expectedUserNotFoundMessage;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUser;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUserNotificationProfile;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUserWithTelegramChatId;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserApiImpl userService;

    @Test
    @DisplayName("getUserNotificationProfile should return profile when user exists")
    void givenExistingUserId_getUserNotificationProfile_shouldReturnProfile() {
        User user = sampleUser();
        UserNotificationProfile expectedProfile = sampleUserNotificationProfile();

        when(userRepository.findById(DEFAULT_USER_ID)).thenReturn(Optional.of(user));

        Optional<UserNotificationProfile> result = userService.getUserNotificationProfile(DEFAULT_USER_ID);

        assertThat(result).contains(expectedProfile);
    }

    @Test
    @DisplayName("getUserNotificationProfile should return empty when user does not exist")
    void givenNonExistentUserId_getUserNotificationProfile_shouldReturnEmpty() {
        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        Optional<UserNotificationProfile> result = userService.getUserNotificationProfile(NON_EXISTENT_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("linkTelegramChatId should update user and save when user exists")
    void givenExistingUserId_linkTelegramChatId_shouldUpdateUserAndSave() {
        User user = sampleUser();
        User expectedUser = sampleUserWithTelegramChatId(UPDATED_TELEGRAM_CHAT_ID);
        when(userRepository.findById(DEFAULT_USER_ID)).thenReturn(Optional.of(user));

        userService.linkTelegramChatId(DEFAULT_USER_ID, UPDATED_TELEGRAM_CHAT_ID);

        assertThat(user).isEqualTo(expectedUser);
        verify(userRepository).save(expectedUser);
    }

    @Test
    @DisplayName("linkTelegramChatId should throw ResourceNotFoundException when user does not exist")
    void givenNonExistentUserId_linkTelegramChatId_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.linkTelegramChatId(NON_EXISTENT_USER_ID, UPDATED_TELEGRAM_CHAT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(expectedUserNotFoundMessage(NON_EXISTENT_USER_ID));

        verifyNoMoreInteractions(userRepository);
    }
}

