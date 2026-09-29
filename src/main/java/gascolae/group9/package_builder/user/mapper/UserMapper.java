package gascolae.group9.package_builder.user.mapper;

import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import gascolae.group9.package_builder.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    User toUser(UserRegisterRequest request);
    UserResponse toUserResponse(User user);
    List<UserResponse> toUserResponse(List<User> users);
}
