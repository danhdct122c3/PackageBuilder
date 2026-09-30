package gascolae.group9.package_builder.config;

import gascolae.group9.package_builder.user.entity.Role;
import gascolae.group9.package_builder.user.entity.User;
import gascolae.group9.package_builder.user.enums.RoleName;
import gascolae.group9.package_builder.user.repository.RoleRepository;
import gascolae.group9.package_builder.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Slf4j
public class ApplicationInitConfig {



    @Bean
    ApplicationRunner initApplicationRunner(RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return  args -> {
            if(userRepository.findByUsername("admin").isEmpty()) {



                Set<Role> roles = new HashSet<>();
                Role adminRole= roleRepository.findById(RoleName.ADMIN)
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .name(RoleName.ADMIN)
                                .description("Admin role")
                                .build()));
                roles.add(adminRole);

                User adminUser = User.builder()
                        .username("admin")
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(roles)
                        .build();
                userRepository.save(adminUser);
                log.info("Admin user created successfully");
            }
        };
    }
}
