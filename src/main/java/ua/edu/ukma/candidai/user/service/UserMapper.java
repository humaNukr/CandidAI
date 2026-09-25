package ua.edu.ukma.candidai.user.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.user.dto.response.UserResponse;
import ua.edu.ukma.candidai.user.model.User;

@Mapper(config = GlobalMapperConfig.class)
interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "id")
    UserNotificationProfile toNotificationProfile(User user);
}
