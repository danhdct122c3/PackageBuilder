package gascolae.group9.package_builder.user.repository;

import gascolae.group9.package_builder.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,String> {

    boolean existsByEmail(String email);



    Optional<User> findByUsername( String username);
}
