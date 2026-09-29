package gascolae.group9.package_builder.user.controller;

import gascolae.group9.package_builder.dto.response.APIResponse;
import gascolae.group9.package_builder.user.dto.request.ChangePasswordRequest;
import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.request.UserUpdateRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import gascolae.group9.package_builder.user.enums.RoleName;
import gascolae.group9.package_builder.user.enums.UserStatus;
import gascolae.group9.package_builder.user.service.UserService;
import jakarta.persistence.EntityListeners;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping("/register")
    public APIResponse<UserResponse> createUser(@Valid @RequestBody UserRegisterRequest request){
        return APIResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }
    @PutMapping("/update/{userId}")
    public APIResponse<Void> updateUser(@Valid @RequestBody UserUpdateRequest request, @PathVariable("userId") String userId){
        userService.updateUser(request, userId);
        return APIResponse.<Void>builder()
                .message("cập nhật thành công")
                .build();
    }

    @GetMapping("/{userId}")
    public APIResponse<UserResponse> findUserById(@PathVariable("userId") String userId){
        return APIResponse.<UserResponse>builder()
                .result(userService.findUserById(userId))
                .build();
    }

    @PutMapping("/update-status/{userId}")
    public APIResponse<Void> updateUserStatus(@PathVariable("userId") String userId, @RequestParam("status") UserStatus status) {

        userService.updateUserStatus(userId, status);
        return APIResponse.<Void>builder()
                .message("cập nhật thành công")
                .build();
    }

    @PutMapping("/update-password/{userId}")
    public APIResponse<Void> updateUserPassword(@PathVariable("userId") String userId, @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return APIResponse.<Void>builder()
                .message("cập nhật thành công")
                .build();
    }

    @PutMapping("/update-role/{userId}")
    public APIResponse<Void> updateUserRole(@PathVariable("userId") String userId, @RequestParam("role") Set<RoleName> role) {
        userService.updateUserRoles(userId,role);

        return APIResponse.<Void>builder()
                .message("cập nhật thành công")
                .build();
    }

    @GetMapping
    public APIResponse<List<UserResponse>> findAllUsers() {
        return APIResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }
}
