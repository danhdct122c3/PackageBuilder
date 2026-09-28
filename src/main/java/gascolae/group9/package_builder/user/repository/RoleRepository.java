package gascolae.group9.package_builder.user.repository;

import gascolae.group9.package_builder.user.entity.Role;
import gascolae.group9.package_builder.user.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface RoleRepository extends JpaRepository<Role, RoleName> {
}
