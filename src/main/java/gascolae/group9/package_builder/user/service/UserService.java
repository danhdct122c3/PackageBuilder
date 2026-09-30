package gascolae.group9.package_builder.user.service;

import gascolae.group9.package_builder.user.dto.request.ChangePasswordRequest;
import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.request.UserUpdateRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import gascolae.group9.package_builder.user.enums.RoleName;
import gascolae.group9.package_builder.user.enums.UserStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

public interface UserService {
    UserResponse createUser(UserRegisterRequest request);
    void updateUser(UserUpdateRequest request, String userId);
    void updateUserStatus(String userId, UserStatus newStatus);
    void changePassword(String userId, ChangePasswordRequest request);
    UserResponse findUserById(String userId);
    List<UserResponse> getUsers();
    void updateUserRoles(String userId, Set<RoleName> newRoles);
    UserResponse getCurrentUser();

//    void updateUserStatus(UserUpdateStatusRequest request, String userId);

//    UserResponse getMyInfo();
}
