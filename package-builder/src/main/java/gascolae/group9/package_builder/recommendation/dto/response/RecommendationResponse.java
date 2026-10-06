package gascolae.group9.package_builder.recommendation.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationResponse {
    String recommendationId;
    String requirementId;
    String serviceId;
    String serviceCode;
    String serviceName;
    String category;
    Integer rankOrder;
    BigDecimal matchScore;
    BigDecimal objectiveScore;
    BigDecimal useCaseScore;
    BigDecimal industryScore;
    BigDecimal outputScore;
    BigDecimal tagScore;
    String recommendationReason;
    Boolean accepted;
    LocalDateTime createdAt;
}
