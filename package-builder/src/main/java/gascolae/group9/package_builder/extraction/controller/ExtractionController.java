package gascolae.group9.package_builder.extraction.controller;

import gascolae.group9.package_builder.dto.response.APIResponse;
import gascolae.group9.package_builder.extraction.dto.request.ExtractRequirementRequest;
import gascolae.group9.package_builder.extraction.dto.request.ExtractTextRequest;
import gascolae.group9.package_builder.extraction.dto.request.RequirementCodesRequest;
import gascolae.group9.package_builder.extraction.dto.response.AiStatusResponse;
import gascolae.group9.package_builder.extraction.dto.response.ExtractionResponse;
import gascolae.group9.package_builder.extraction.dto.response.RequirementCodesResponse;
import gascolae.group9.package_builder.extraction.service.RequirementExtractionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

/**
 * D6 — AI trích xuất câu nhu cầu thành mã. Mọi endpoint cần JWT: frontend không bao giờ cầm Gemini key,
 * mọi lời gọi Gemini đi qua backend.
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExtractionController {
    RequirementExtractionService extractionService;

    /** Thử trích xuất một câu, không lưu DB. */
    @PostMapping("/ai/extract")
    public APIResponse<ExtractionResponse> preview(@Valid @RequestBody ExtractTextRequest request) {
        return APIResponse.<ExtractionResponse>builder()
                .result(extractionService.preview(request.getText()))
                .build();
    }

    /** Trích xuất cho requirement DRAFT và lưu (body có thể bỏ trống để dùng câu/form đã lưu). */
    @PostMapping("/requirements/{id}/extract")
    public APIResponse<ExtractionResponse> extract(
            @PathVariable("id") String id,
            @Valid @RequestBody(required = false) ExtractRequirementRequest request) {
        return APIResponse.<ExtractionResponse>builder()
                .result(extractionService.extractForRequirement(id, request))
                .build();
    }

    @GetMapping("/requirements/{id}/codes")
    public APIResponse<RequirementCodesResponse> getCodes(@PathVariable("id") String id) {
        return APIResponse.<RequirementCodesResponse>builder()
                .result(extractionService.getCodes(id))
                .build();
    }

    /** Sales sửa mã trên phiếu trước khi bấm xác nhận. */
    @PutMapping("/requirements/{id}/codes")
    public APIResponse<RequirementCodesResponse> updateCodes(
            @PathVariable("id") String id,
            @RequestBody RequirementCodesRequest request) {
        return APIResponse.<RequirementCodesResponse>builder()
                .result(extractionService.updateCodes(id, request))
                .build();
    }

    /** Trạng thái cấu hình AI; ?ping=true để gọi thử Gemini (tốn 1 lượt gọi). */
    @GetMapping("/ai/status")
    public APIResponse<AiStatusResponse> status(@RequestParam(value = "ping", defaultValue = "false") boolean ping) {
        return APIResponse.<AiStatusResponse>builder()
                .result(extractionService.status(ping))
                .build();
    }
}
