package gascolae.group9.package_builder.user.mapper;

import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import gascolae.group9.package_builder.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserRegisterRequest request);
    UserResponse toUserResponse(User user);
}
