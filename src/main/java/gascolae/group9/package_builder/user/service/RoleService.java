package gascolae.group9.package_builder.user.service;

import gascolae.group9.package_builder.user.dto.request.RoleCreateRequest;
import gascolae.group9.package_builder.user.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
    RoleResponse createRole(RoleCreateRequest request);
    List<RoleResponse> getAllRole();
}
