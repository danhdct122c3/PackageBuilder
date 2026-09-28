package gascolae.group9.package_builder.user.service;

import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserRegisterRequest request);
//    UserResponse updateUser(UserUpdateRequest request, String userId);
//    List<UserResponse> getUsers();
//    UserResponse getUserById(String userId);

//    void assignRoleToUser(UserAssignRoleRequest request, String userId, String role);

//    void updateUserStatus(UserUpdateStatusRequest request, String userId);

//    UserResponse getMyInfo();
}
