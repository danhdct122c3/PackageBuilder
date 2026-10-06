package gascolae.group9.package_builder.extraction.engine;

/**
 * Ngưỡng tin cậy của D6 (BAN_GIAO_SV1 mục 6, hàm decide() trong pipeline_demo.py).
 */
public final class ConfidencePolicy {
    public static final double HIGH = 0.75;
    public static final double LOW = 0.4;

    public enum Decision {
        /** Điền sẵn phiếu, Sales chỉ cần xem lại rồi xác nhận. */
        HIGH,
        /** Sales sửa hoặc bổ sung các trường chưa chắc rồi xác nhận. */
        NEED_CONFIRM,
        /** Không chạy gợi ý, Sales hỏi thêm khách. */
        INSUFFICIENT
    }

    public record Verdict(Decision decision, String reason) {
    }

    private ConfidencePolicy() {
    }

    public static Verdict decide(ExtractionResult r) {
        double overall = r.overall();
        if (r.codes(ExtractionResult.OBJECTIVE).isEmpty()) {
            return new Verdict(Decision.INSUFFICIENT,
                    "Không có mã mục tiêu: không chạy gợi ý. Sales hỏi thêm khách mục tiêu là gì, hiện trường ở đâu, muốn nhận gì.");
        }
        if (r.isFallback()) {
            return new Verdict(Decision.NEED_CONFIRM,
                    "Trích bằng bảng từ khóa dự phòng: Sales phải kiểm tra và xác nhận phiếu trước khi chạy gợi ý.");
        }
        if (overall >= HIGH) {
            return new Verdict(Decision.HIGH,
                    "Độ tin cậy " + overall + " ≥ " + HIGH + ": điền sẵn phiếu, Sales chỉ cần xem lại.");
        }
        if (overall >= LOW) {
            return new Verdict(Decision.NEED_CONFIRM,
                    "Độ tin cậy " + overall + " (0.4–0.75): Sales sửa hoặc bổ sung các trường chưa chắc rồi xác nhận.");
        }
        return new Verdict(Decision.INSUFFICIENT,
                "Độ tin cậy " + overall + " < 0.4: không chạy gợi ý, Sales hỏi thêm khách.");
    }
}
