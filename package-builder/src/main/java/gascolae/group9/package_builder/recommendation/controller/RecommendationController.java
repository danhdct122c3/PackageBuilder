package gascolae.group9.package_builder.recommendation.controller;

import gascolae.group9.package_builder.dto.response.APIResponse;
import gascolae.group9.package_builder.recommendation.dto.response.RecommendationResponse;
import gascolae.group9.package_builder.recommendation.dto.response.RecommendationRunResponse;
import gascolae.group9.package_builder.recommendation.service.RecommendationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** D4 — chấm điểm và gợi ý dịch vụ cho requirement đã xác nhận. */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationController {
    RecommendationService recommendationService;

    /** Sales bấm "Phân tích & Gợi ý dịch vụ". Chạy lại sẽ thay kết quả cũ (giữ các dịch vụ đã chọn). */
    @PostMapping("/requirements/{id}/recommendations")
    public APIResponse<RecommendationRunResponse> run(@PathVariable("id") String id) {
        return APIResponse.<RecommendationRunResponse>builder()
                .result(recommendationService.runRecommendation(id))
                .build();
    }

    @GetMapping("/requirements/{id}/recommendations")
    public APIResponse<List<RecommendationResponse>> list(@PathVariable("id") String id) {
        return APIResponse.<List<RecommendationResponse>>builder()
                .result(recommendationService.getRecommendations(id))
                .build();
    }

    /** "+ Thêm vào Gói giải pháp" → accepted = true; gửi accepted=false để bỏ chọn. */
    @PutMapping("/recommendations/{id}/accept")
    public APIResponse<RecommendationResponse> accept(
            @PathVariable("id") String id,
            @RequestParam(value = "accepted", defaultValue = "true") boolean accepted) {
        return APIResponse.<RecommendationResponse>builder()
                .result(recommendationService.setAccepted(id, accepted))
                .build();
    }
}
