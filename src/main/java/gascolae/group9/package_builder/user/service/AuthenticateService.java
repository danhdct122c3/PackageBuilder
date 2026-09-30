package gascolae.group9.package_builder.user.service;

import com.nimbusds.jose.JOSEException;
import gascolae.group9.package_builder.user.dto.request.IntrospectRequest;
import gascolae.group9.package_builder.user.dto.request.LoginRequest;
import gascolae.group9.package_builder.user.dto.request.LogoutRequest;
import gascolae.group9.package_builder.user.dto.response.IntrospectResponse;
import gascolae.group9.package_builder.user.dto.response.LoginResponse;

import java.text.ParseException;

public interface AuthenticateService {
    LoginResponse authenticate(LoginRequest request);
    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
    void logout(LogoutRequest request) throws ParseException, JOSEException;
}
