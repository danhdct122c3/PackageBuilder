package gascolae.group9.package_builder.recommendation.service;

import gascolae.group9.package_builder.recommendation.dto.response.RecommendationResponse;
import gascolae.group9.package_builder.recommendation.dto.response.RecommendationRunResponse;

import java.util.List;

public interface RecommendationService {
    /** Chấm điểm 12 dịch vụ cho requirement đã CONFIRMED và lưu shortlist vào service_recommendations. */
    RecommendationRunResponse runRecommendation(String requirementId);

    List<RecommendationResponse> getRecommendations(String requirementId);

    /** Sales chọn / bỏ chọn dịch vụ để đưa vào gói giải pháp. */
    RecommendationResponse setAccepted(String recommendationId, boolean accepted);
}
