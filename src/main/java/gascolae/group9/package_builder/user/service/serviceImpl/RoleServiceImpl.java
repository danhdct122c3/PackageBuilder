package gascolae.group9.package_builder.user.service.serviceImpl;

import gascolae.group9.package_builder.exception.AppException;
import gascolae.group9.package_builder.exception.ErrorCode;
import gascolae.group9.package_builder.user.dto.request.RoleCreateRequest;
import gascolae.group9.package_builder.user.dto.response.RoleResponse;
import gascolae.group9.package_builder.user.entity.Role;
import gascolae.group9.package_builder.user.enums.RoleName;
import gascolae.group9.package_builder.user.repository.RoleRepository;
import gascolae.group9.package_builder.user.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor // tự động tạo constructor cho tất cả các field final
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // tự động set tất cả các field là private và final
@Slf4j
public class RoleServiceImpl  implements RoleService {

    RoleRepository roleRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public RoleResponse createRole(RoleCreateRequest request) {
        RoleName roleName;

        try {
            roleName = RoleName.valueOf(request.getName().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        if (roleRepository.existsById(roleName)) {
            log.error("Role with name {} already exists", roleName);
            throw new AppException(ErrorCode.ROLE_EXISTED);
        }

        Role role = Role.builder()
                .name(roleName)
                .description(request.getDescription())
                .build();

        Role savedRole = roleRepository.save(role);

        return RoleResponse.builder()
                .name(savedRole.getName().name())
                .description(savedRole.getDescription())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')") // Chỉ cho phép người dùng có role ADMIN thực hiện phương thức này
    public List<RoleResponse> getAllRole() {
        var roles = roleRepository.findAll();
        return roles.stream()
                .map(role -> RoleResponse.builder()
                        .name(role.getName().toString())
                        .description(role.getDescription())
                        .build())
                .toList();
    }



}
