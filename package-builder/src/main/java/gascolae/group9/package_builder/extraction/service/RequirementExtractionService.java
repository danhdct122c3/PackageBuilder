package gascolae.group9.package_builder.extraction.service;

import gascolae.group9.package_builder.extraction.dto.request.ExtractRequirementRequest;
import gascolae.group9.package_builder.extraction.dto.request.RequirementCodesRequest;
import gascolae.group9.package_builder.extraction.dto.response.AiStatusResponse;
import gascolae.group9.package_builder.extraction.dto.response.ExtractionResponse;
import gascolae.group9.package_builder.extraction.dto.response.RequirementCodesResponse;

public interface RequirementExtractionService {
    /** Chỉ trích xuất và trả phiếu, không lưu DB (Sales thử câu nhu cầu). */
    ExtractionResponse preview(String text);

    /** Trích xuất cho requirement DRAFT và lưu kết quả (requirement_tags, expected outputs, extraction_*). */
    ExtractionResponse extractForRequirement(String requirementId, ExtractRequirementRequest request);

    RequirementCodesResponse getCodes(String requirementId);

    /** Sales sửa mã trên phiếu trước khi xác nhận. */
    RequirementCodesResponse updateCodes(String requirementId, RequirementCodesRequest request);

    AiStatusResponse status(boolean ping);
}
