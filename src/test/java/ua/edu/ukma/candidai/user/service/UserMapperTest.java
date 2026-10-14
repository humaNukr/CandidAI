package ua.edu.ukma.candidai.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.user.dto.request.UpdateUserRequest;
import ua.edu.ukma.candidai.user.dto.response.UserResponse;
import ua.edu.ukma.candidai.user.model.User;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.user.UserTestResources.*;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("toResponse should map all fields from User entity")
    void givenUser_toResponse_shouldMapAllFields() {
        User user = sampleUser();
        UserResponse expected = sampleUserResponse();

        UserResponse actual = mapper.toResponse(user);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("toNotificationProfile should map userId and notification contact fields")
    void givenUser_toNotificationProfile_shouldMapUserIdAndNotificationFields() {
        User user = sampleUser();
        UserNotificationProfile expected = sampleUserNotificationProfile();

        UserNotificationProfile actual = mapper.toNotificationProfile(user);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserFromRequest should update non-null fields in place")
    void givenFullUpdateRequest_updateUserFromRequest_shouldUpdateAllSpecifiedFields() {
        User user = sampleUser();
        UpdateUserRequest request = sampleUpdateUserRequest();
        User expected = sampleUpdatedUser();

        mapper.updateUserFromRequest(request, user);

        assertThat(user).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserFromRequest should ignore null fields during partial update")
    void givenPartialUpdateRequest_updateUserFromRequest_shouldIgnoreNullFields() {
        User user = sampleUser();
        UpdateUserRequest request = samplePartialUpdateUserRequest();
        User expected = sampleUserWithUpdatedFullName();

        mapper.updateUserFromRequest(request, user);

        assertThat(user).usingRecursiveComparison().isEqualTo(expected);
    }
}
