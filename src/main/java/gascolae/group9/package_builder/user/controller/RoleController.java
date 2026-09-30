package gascolae.group9.package_builder.user.controller;

import gascolae.group9.package_builder.dto.response.APIResponse;
import gascolae.group9.package_builder.user.dto.request.RoleCreateRequest;
import gascolae.group9.package_builder.user.dto.response.RoleResponse;
import gascolae.group9.package_builder.user.service.RoleService;
import jakarta.persistence.EntityListeners;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;

    @PostMapping("/create")
    public APIResponse<RoleResponse> createRole(@RequestBody @Valid RoleCreateRequest request){
        return APIResponse.<RoleResponse>builder()
                .result(roleService.createRole(request))
                .build();
    }

    @GetMapping
    public APIResponse<List<RoleResponse>> getAllRole(){
        return APIResponse.<List<RoleResponse>>builder()
                .result(roleService.getAllRole())
                .build();
    }
}
