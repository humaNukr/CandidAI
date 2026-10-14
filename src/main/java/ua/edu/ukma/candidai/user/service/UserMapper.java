package ua.edu.ukma.candidai.user.service;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ua.edu.ukma.candidai.common.config.GlobalMapperConfig;
import ua.edu.ukma.candidai.user.UserNotificationProfile;
import ua.edu.ukma.candidai.user.dto.request.UpdateUserRequest;
import ua.edu.ukma.candidai.user.dto.response.UserResponse;
import ua.edu.ukma.candidai.user.model.User;

@Mapper(config = GlobalMapperConfig.class)
interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "id")
    UserNotificationProfile toNotificationProfile(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "email", ignore = true)
    void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
