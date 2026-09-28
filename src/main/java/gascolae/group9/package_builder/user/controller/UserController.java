package gascolae.group9.package_builder.user.controller;

import gascolae.group9.package_builder.dto.response.APIResponse;
import gascolae.group9.package_builder.user.dto.request.UserRegisterRequest;
import gascolae.group9.package_builder.user.dto.response.UserResponse;
import gascolae.group9.package_builder.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping("/register")
    public APIResponse<UserResponse> createUser(UserRegisterRequest request){
        return APIResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

}
