package gascolae.group9.package_builder.user.service.serviceImpl;

import gascolae.group9.package_builder.exception.AppException;
import gascolae.group9.package_builder.exception.ErrorCode;
import gascolae.group9.package_builder.user.dto.request.ChangePasswordRequest;
import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.request.UserUpdateRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import gascolae.group9.package_builder.user.entity.Role;
import gascolae.group9.package_builder.user.entity.User;
import gascolae.group9.package_builder.user.enums.RoleName;
import gascolae.group9.package_builder.user.enums.UserStatus;
import gascolae.group9.package_builder.user.mapper.UserMapper;
import gascolae.group9.package_builder.user.repository.RoleRepository;
import gascolae.group9.package_builder.user.repository.UserRepository;
import gascolae.group9.package_builder.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor // tự động tạo constructor cho tất cả các field final
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // tự động set tất cả các field là private và final
@Slf4j
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserRegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) throw new AppException(ErrorCode.USER_EMAIL_EXISTED);

        User toUser= userMapper.toUser(request);

        toUser.setPassword(passwordEncoder.encode(request.getPassword()));
        Set<Role> roles = new HashSet<>();
//        Role role = roleRepository.save(Role.builder().name(RoleName.USER).build());
        Role role= roleRepository.findById(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.USER)
                        .description("Default user role")
                        .build()));
        roles.add(role);
        toUser.setRole(roles);
        toUser.setUserStatus(UserStatus.ACTIVE);

        return userMapper.toUserResponse(userRepository.save(toUser));

    }

    public void updateUser(UserUpdateRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if(request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }


        if(request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        userRepository.save(user);
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    @PreAuthorize("hasRole('ADMIN')") // kiểm tra xem người dùng có vai trò ADMIN hay không trước khi thực hiện phương thức này
    public List<UserResponse> getUsers() {

        var authentication= SecurityContextHolder.getContext().getAuthentication();
        log.info("username: " + authentication.getName());
        authentication.getAuthorities().forEach(authority -> log.info(authority.getAuthority()));

        List<User> users = userRepository.findAll();
        return userMapper.toUserResponse(users);
    }
    @PostAuthorize("returnObject.username == authentication.name or hasRole('ADMIN')") // kiểm tra xem người dùng có quyền truy cập vào đối tượng trả về hay không
    public UserResponse findUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    public void updateUserStatus(String userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if(newStatus == null) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        user.setUserStatus(newStatus);
        userRepository.save(user);
    }

    public void updateUserRoles(String userId, Set<RoleName> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (newRoles == null || newRoles.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }
        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : newRoles) {
            Role role = roleRepository.findById(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(roleName)
                            .description("Role: " + roleName)
                            .build()));
            roles.add(role);
        }

        user.setRole(roles);
        userRepository.save(user);
    }

    public UserResponse getCurrentUser() {
        var authentication= SecurityContextHolder.getContext().getAuthentication(); // lấy thông tin xác thực của người dùng hiện tại từ SecurityContextHolder
        String currentUserId = authentication.getName();
        User user = userRepository.findByUsername(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }
}
