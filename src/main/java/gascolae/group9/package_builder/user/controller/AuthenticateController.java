package gascolae.group9.package_builder.user.controller;

import com.nimbusds.jose.JOSEException;
import gascolae.group9.package_builder.dto.response.APIResponse;
import gascolae.group9.package_builder.user.dto.request.IntrospectRequest;
import gascolae.group9.package_builder.user.dto.request.LoginRequest;
import gascolae.group9.package_builder.user.dto.request.LogoutRequest;
import gascolae.group9.package_builder.user.dto.response.IntrospectResponse;
import gascolae.group9.package_builder.user.dto.response.LoginResponse;
import gascolae.group9.package_builder.user.service.AuthenticateService;
import jakarta.persistence.EntityListeners;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@Slf4j
@RestController
@RequestMapping("/authenticate")
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticateController {
    AuthenticateService authenticateService;

    @PostMapping("/login")
    public APIResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request){
        return APIResponse.<LoginResponse>builder()
                .message("Login successful")
                .result(authenticateService.authenticate(request))
                .build();

    }
    @PostMapping("/introspect")
    public APIResponse<IntrospectResponse> introspect(@RequestBody @Valid IntrospectRequest request) throws ParseException, JOSEException {
        return APIResponse.<IntrospectResponse>builder()
                .message("Introspect successful")
                .result(authenticateService.introspect(request))
                .build();
    }

    @PostMapping("/logout")
    public APIResponse<Void> logout(@RequestBody @Valid LogoutRequest request) throws ParseException, JOSEException {
        authenticateService.logout(request);
        return APIResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }
}
