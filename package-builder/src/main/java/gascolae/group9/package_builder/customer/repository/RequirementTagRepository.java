package gascolae.group9.package_builder.customer.repository;

import gascolae.group9.package_builder.customer.entity.RequirementTag;
import gascolae.group9.package_builder.customer.entity.RequirementTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementTagRepository extends JpaRepository<RequirementTag, RequirementTagId> {

    @Query("select rt from RequirementTag rt join fetch rt.tag where rt.requirement.requirementId = :requirementId")
    List<RequirementTag> findWithTagByRequirementId(@Param("requirementId") String requirementId);

    @Modifying(flushAutomatically = true)
    @Query("delete from RequirementTag rt where rt.requirement.requirementId = :requirementId")
    int deleteByRequirementId(@Param("requirementId") String requirementId);
}
