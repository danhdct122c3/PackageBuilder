package gascolae.group9.package_builder.user.service.serviceImpl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import gascolae.group9.package_builder.exception.AppException;
import gascolae.group9.package_builder.exception.ErrorCode;
import gascolae.group9.package_builder.user.dto.request.IntrospectRequest;
import gascolae.group9.package_builder.user.dto.request.LoginRequest;
import gascolae.group9.package_builder.user.dto.request.LogoutRequest;
import gascolae.group9.package_builder.user.dto.response.IntrospectResponse;
import gascolae.group9.package_builder.user.dto.response.LoginResponse;
import gascolae.group9.package_builder.user.entity.InvalidatedToken;
import gascolae.group9.package_builder.user.entity.User;
import gascolae.group9.package_builder.user.enums.UserStatus;
import gascolae.group9.package_builder.user.repository.InvalidatedTokenRepository;
import gascolae.group9.package_builder.user.repository.UserRepository;
import gascolae.group9.package_builder.user.service.AuthenticateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
 // tự động tạo constructor cho tất cả các field final
@FieldDefaults(level = AccessLevel.PRIVATE) // tự động set tất cả các field là private và final
@Slf4j
public class AuthenticateServiceImpl implements AuthenticateService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String signerKey;
    private final long validDuration;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    public AuthenticateServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            InvalidatedTokenRepository invalidatedTokenRepository,
            @Value("${jwt.signerKey}") String signerKey,
            @Value("${jwt.valid-duration}") long validDuration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
        this.signerKey = signerKey;
        this.validDuration = validDuration;
    }

    public LoginResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername()) //check if user exists
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))  //check if password is correct
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);

        if (user.getUserStatus() == UserStatus.INACTIVE) throw new  //check if user is active
                AppException(ErrorCode.USER_NOT_ACTIVE);
        var token = generateToken(user);
        return LoginResponse.builder()
                .accessToken(token)
                .isAuthenticated(true)
                .build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).type(JOSEObjectType.JWT).build();
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("package-builder")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plusSeconds(validDuration).toEpochMilli()
                ))
//                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
//                .claim("scope", user.getRole().stream().map(role -> role.getName().name()).toList())
                .claim("scope", buildScope(user))
                .jwtID(UUID.randomUUID().toString())
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
       try {
           verifyToken(token);
       } catch (AppException e) {
           return IntrospectResponse.builder()
                   .valid(false)
                   .build();
       }
       
        return IntrospectResponse.builder()
                .valid(true)
                .build();

    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRole())) {
            user.getRole().forEach(role ->
                    stringJoiner.add(role.getName().name())
            );
        }

        return stringJoiner.toString();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        // Invalidate the token if you are storing it in a database or cache
        // For stateless JWT, you can't invalidate the token on the server side
        // You can implement a token blacklist or use short-lived tokens with refresh tokens
        var SignedJWT = verifyToken(request.getToken());
        String jit = SignedJWT.getJWTClaimsSet().getJWTID();
        Date expirationTime = SignedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expirationTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
    }

    private SignedJWT verifyToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);
        if(!(verified && expirationTime.after(new Date()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return signedJWT;
    }
}