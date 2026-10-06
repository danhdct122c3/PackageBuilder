package gascolae.group9.package_builder.recommendation.repository;

import gascolae.group9.package_builder.recommendation.entity.ServiceRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRecommendationRepository extends JpaRepository<ServiceRecommendation, String> {

    @Query("select r from ServiceRecommendation r join fetch r.service " +
            "where r.requirement.requirementId = :requirementId order by r.rankOrder asc")
    List<ServiceRecommendation> findByRequirementIdOrderByRank(@Param("requirementId") String requirementId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ServiceRecommendation r where r.requirement.requirementId = :requirementId")
    int deleteByRequirementId(@Param("requirementId") String requirementId);
}
