package ua.edu.ukma.candidai.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua.edu.ukma.candidai.user.UserApi;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserApiImpl implements UserApi {

    private final UserRepository userRepository;

    @Override
    public Optional<UserNotificationProfile> getUserNotificationProfile(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId)
                .map(user -> new UserNotificationProfile(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getTelegramChatId()
                ));
    }
}
