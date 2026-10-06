package gascolae.group9.package_builder.recommendation.service.serviceImpl;

import gascolae.group9.package_builder.catalog.entity.DataItem;
import gascolae.group9.package_builder.catalog.entity.Service;
import gascolae.group9.package_builder.catalog.repository.DataItemRepository;
import gascolae.group9.package_builder.catalog.repository.ServiceRepository;
import gascolae.group9.package_builder.customer.entity.CustomerRequirement;
import gascolae.group9.package_builder.customer.entity.RequirementExpectedOutput;
import gascolae.group9.package_builder.customer.enums.RequirementStatus;
import gascolae.group9.package_builder.customer.repository.CustomerRequirementRepository;
import gascolae.group9.package_builder.customer.repository.RequirementTagRepository;
import gascolae.group9.package_builder.exception.AppException;
import gascolae.group9.package_builder.exception.ErrorCode;
import gascolae.group9.package_builder.extraction.service.AiResources;
import gascolae.group9.package_builder.recommendation.dto.response.RecommendationResponse;
import gascolae.group9.package_builder.recommendation.dto.response.RecommendationRunResponse;
import gascolae.group9.package_builder.recommendation.engine.RecommendationScorer;
import gascolae.group9.package_builder.recommendation.entity.ServiceRecommendation;
import gascolae.group9.package_builder.recommendation.repository.ServiceRecommendationRepository;
import gascolae.group9.package_builder.recommendation.service.CatalogSnapshotLoader;
import gascolae.group9.package_builder.recommendation.service.RecommendationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    CustomerRequirementRepository requirementRepository;
    RequirementTagRepository requirementTagRepository;
    DataItemRepository dataItemRepository;
    ServiceRepository serviceRepository;
    ServiceRecommendationRepository recommendationRepository;
    CatalogSnapshotLoader catalogSnapshotLoader;
    AiResources aiResources;

    @Override
    @Transactional
    public RecommendationRunResponse runRecommendation(String requirementId) {
        CustomerRequirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUIREMENT_NOT_FOUND));
        if (requirement.getStatus() != RequirementStatus.CONFIRMED) {
            throw new AppException(ErrorCode.REQUIREMENT_NOT_CONFIRMED);
        }

        Map<String, List<String>> codes = requirementCodes(requirement);

        // Giữ lựa chọn "accepted" của Sales nếu dịch vụ vẫn còn trong shortlist mới.
        Set<String> previouslyAccepted = recommendationRepository.findByRequirementIdOrderByRank(requirementId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getAccepted()))
                .map(r -> r.getService().getServiceId())
                .collect(Collectors.toSet());
        recommendationRepository.deleteByRequirementId(requirementId);

        RecommendationRunResponse.RecommendationRunResponseBuilder out = RecommendationRunResponse.builder()
                .requirementId(requirementId)
                .requirementCode(requirement.getRequirementCode())
                .requirementCodes(codes)
                .matchingRulesVersion(aiResources.getMatchingRules().version());

        if (codes.get("objective").isEmpty()) {
            log.info("D4 bỏ qua requirement {}: không có mã mục tiêu", requirement.getRequirementCode());
            return out.ran(false)
                    .message("Không đủ căn cứ: phiếu chưa có mục tiêu nên không chạy gợi ý. "
                            + "Sales hỏi thêm khách mục tiêu là gì, hiện trường ở đâu, muốn nhận gì.")
                    .recommendations(List.of())
                    .build();
        }

        RecommendationScorer scorer = new RecommendationScorer(aiResources.getMatchingRules(), catalogSnapshotLoader.load());
        RecommendationScorer.Result result = scorer.score(new RecommendationScorer.Requirement(codes));

        List<ServiceRecommendation> rows = new ArrayList<>();
        int rank = 1;
        for (RecommendationScorer.ScoreRow row : result.shortlist()) {
            Service service = serviceRepository.getReferenceById(row.getService().serviceId());
            rows.add(ServiceRecommendation.builder()
                    .requirement(requirement)
                    .service(service)
                    .matchScore(score(row.getMatchScore()))
                    .objectiveScore(score(row.getObjectiveScore()))
                    .useCaseScore(score(row.getUseCaseScore()))
                    .industryScore(score(row.getIndustryScore()))
                    .outputScore(score(row.getOutputScore()))
                    .tagScore(score(row.getTagScore()))
                    .recommendationReason(row.getReason())
                    .rankOrder(rank++)
                    .accepted(previouslyAccepted.contains(row.getService().serviceId()))
                    .build());
        }
        List<ServiceRecommendation> saved = recommendationRepository.saveAll(rows);
        log.info("D4 requirement {}: chấm {} dịch vụ, shortlist {}", requirement.getRequirementCode(),
                result.all().size(), result.shortlist().stream().map(RecommendationScorer.ScoreRow::getServiceCode).toList());

        return out.ran(true)
                .message(saved.isEmpty()
                        ? "Không có dịch vụ nào đạt ngưỡng " + aiResources.getMatchingRules().minMatchScore() + " điểm"
                        : "Đã gợi ý " + saved.size() + " dịch vụ")
                .servicesScored(result.all().size())
                .recommendations(saved.stream().map(this::toResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(String requirementId) {
        if (!requirementRepository.existsById(requirementId)) {
            throw new AppException(ErrorCode.REQUIREMENT_NOT_FOUND);
        }
        return recommendationRepository.findByRequirementIdOrderByRank(requirementId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecommendationResponse setAccepted(String recommendationId, boolean accepted) {
        ServiceRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new AppException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        rec.setAccepted(accepted);
        return toResponse(recommendationRepository.save(rec));
    }

    /** 5 mảng mã theo tín hiệu D4: tag theo loại + đầu ra kỳ vọng đã chuẩn hóa (data_item_id). */
    private Map<String, List<String>> requirementCodes(CustomerRequirement requirement) {
        Map<String, List<String>> codes = new LinkedHashMap<>();
        for (String s : List.of("objective", "environment", "industry", "topic", "expected_output")) {
            codes.put(s, new ArrayList<>());
        }
        requirementTagRepository.findWithTagByRequirementId(requirement.getRequirementId()).forEach(rt -> {
            String signal = switch (rt.getTag().getTagType()) {
                case OBJECTIVE -> "objective";
                case ENVIRONMENT -> "environment";
                case INDUSTRY -> "industry";
                case TOPIC -> "topic";
                default -> null;
            };
            if (signal != null) {
                codes.get(signal).add(rt.getTag().getTagCode());
            }
        });
        List<String> dataItemIds = requirement.getExpectedOutputs().stream()
                .map(RequirementExpectedOutput::getDataItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!dataItemIds.isEmpty()) {
            Map<String, String> idToCode = dataItemRepository.findAllById(dataItemIds).stream()
                    .collect(Collectors.toMap(DataItem::getDataItemId, DataItem::getDataCode));
            dataItemIds.stream().map(idToCode::get).filter(Objects::nonNull).forEach(codes.get("expected_output")::add);
        }
        return codes;
    }

    /** Tín hiệu không có dữ liệu (null) lưu 0 vì cột NOT NULL. */
    private static BigDecimal score(Double v) {
        return BigDecimal.valueOf(v == null ? 0d : v).setScale(2, RoundingMode.HALF_UP);
    }

    private RecommendationResponse toResponse(ServiceRecommendation r) {
        Service s = r.getService();
        return RecommendationResponse.builder()
                .recommendationId(r.getRecommendationId())
                .requirementId(r.getRequirement().getRequirementId())
                .serviceId(s.getServiceId())
                .serviceCode(s.getServiceCode())
                .serviceName(s.getServiceName())
                .category(s.getCategory())
                .rankOrder(r.getRankOrder())
                .matchScore(r.getMatchScore())
                .objectiveScore(r.getObjectiveScore())
                .useCaseScore(r.getUseCaseScore())
                .industryScore(r.getIndustryScore())
                .outputScore(r.getOutputScore())
                .tagScore(r.getTagScore())
                .recommendationReason(r.getRecommendationReason())
                .accepted(r.getAccepted())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
