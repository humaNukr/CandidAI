package ua.edu.ukma.candidai.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.user.model.User;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.user.UserTestResources.DEFAULT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.NON_EXISTENT_USER_ID;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUser;
import static ua.edu.ukma.candidai.user.UserTestResources.sampleUserNotificationProfile;

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
        verify(userRepository).findById(DEFAULT_USER_ID);
    }

    @Test
    @DisplayName("getUserNotificationProfile should return empty when user does not exist")
    void givenNonExistentUserId_getUserNotificationProfile_shouldReturnEmpty() {
        when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

        Optional<UserNotificationProfile> result = userService.getUserNotificationProfile(NON_EXISTENT_USER_ID);

        assertThat(result).isEmpty();
        verify(userRepository).findById(NON_EXISTENT_USER_ID);
    }
}
