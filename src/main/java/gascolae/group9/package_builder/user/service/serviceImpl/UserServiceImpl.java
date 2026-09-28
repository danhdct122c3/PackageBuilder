package gascolae.group9.package_builder.user.service.serviceImpl;

import gascolae.group9.package_builder.exception.AppException;
import gascolae.group9.package_builder.exception.ErrorCode;
import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
}
