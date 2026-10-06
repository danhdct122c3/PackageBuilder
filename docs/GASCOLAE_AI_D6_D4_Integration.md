# Tích hợp AI vào backend: D6 AI Extraction + D4 Recommendation

- **Nguồn:** bàn giao của SV3 (`BAN_GIAO_SV1.md`, D6 v1.4 prompt chốt, D4 v1.1), ngày 05/10/2026
- **Module mới:** `extraction/` (D6), `recommendation/` (D4), bảng `requirement_tags`, `service_recommendations`
- **Cấu hình AI:** `src/main/resources/ai/` (copy nguyên từ SV3, không sửa tay)

---

## 1. Luồng nghiệp vụ

```
Landing form / Sales nhập câu nhu cầu
        │  POST /requirements/{id}/extract
        ▼
[Gemini model chính] ─lỗi─► [model dự phòng] ─lỗi─► [bảng từ khóa, overall = 0.3]
        └────────► Hậu kiểm: gộp mã trùng → loại mã ngoài danh sách (schema + DB) → kiểm schema
                                │
                    Áp ngưỡng: HIGH (≥0.75) · NEED_CONFIRM (0.4–0.75 hoặc bảng từ khóa) · INSUFFICIENT
                                │
        Lưu: requirement_tags, requirement_expected_outputs (data_item_id), extraction_method = AI, extraction_confidence
                                │
        Sales xem/sửa mã (GET/PUT /requirements/{id}/codes) → PUT /requirements/{id}/confirm
                                │
        POST /requirements/{id}/recommendations → D4 chấm 12 dịch vụ → service_recommendations
                                │
        PUT /recommendations/{id}/accept → accepted = TRUE (đưa vào gói ở Tuần 3)
```

## 2. Cấu hình API key (bắt buộc đọc)

1. Copy `package-builder/.env.example` thành `package-builder/.env`, điền `GEMINI_API_KEY=...` (không đặt trong dấu nháy).
   `.env` đã nằm trong `.gitignore` và `.dockerignore`.
2. App tự đọc `.env` (`spring.config.import`), dù chạy bằng `mvnw`, IntelliJ hay VS Code (thư mục gốc repo).
3. Docker: `docker compose up` tự lấy `GEMINI_API_KEY` từ `.env` cạnh `docker-compose.yml`.
4. Không có key → hệ thống **vẫn chạy**, mọi lần trích xuất dùng bảng từ khóa (overall = 0.3, luôn cần Sales xác nhận).
5. Kiểm tra: `GET /api/ai/status` (không gọi mạng) hoặc `GET /api/ai/status?ping=true` (gọi thử Gemini, tốn 1 lượt).
6. Đổi thứ tự model khi `gemini-3.8-flash` quá tải: đặt `GEMINI_MODELS=gemini-3.1-flash-lite,gemini-3.8-flash` trong `.env`.

Key không bao giờ xuất hiện trong response, log hay thông báo lỗi (đã che dạng `AIza…xy`).

## 3. API mới (đều cần JWT, context-path `/api`)

| Method | Path | Việc |
|---|---|---|
| POST | `/ai/extract` | Thử trích xuất 1 câu `{ "text": "..." }`, không lưu DB |
| POST | `/requirements/{id}/extract` | Trích xuất cho requirement DRAFT và lưu. Body `{ "text": "..." }` hoặc bỏ trống |
| GET | `/requirements/{id}/codes` | Các mã đang lưu (mục tiêu, hiện trường, ngành, chủ đề, đầu ra) kèm tên tiếng Việt |
| PUT | `/requirements/{id}/codes` | Sales sửa mã trước khi xác nhận |
| GET | `/ai/status` | Trạng thái cấu hình AI (`?ping=true` để gọi thử) |
| POST | `/requirements/{id}/recommendations` | Chạy D4 (requirement phải CONFIRMED) |
| GET | `/requirements/{id}/recommendations` | Danh sách gợi ý đã lưu, theo `rank_order` |
| PUT | `/recommendations/{id}/accept?accepted=true` | Sales chọn/bỏ chọn dịch vụ |

**Chọn chế độ trích xuất** ở `POST /requirements/{id}/extract`:
- Có `text` trong body → chế độ **TEXT**: lưu câu vào `raw_requirement_text`, điền `objective_raw`, `industry_raw`, `environment_raw` (cắt 100 ký tự), diện tích.
- Không có `text` nhưng requirement đã có `raw_requirement_text` → TEXT với câu đã lưu.
- Cả hai đều trống (lead từ landing form) → chế độ **FORM**: gửi AI các trường mục tiêu, đầu ra mong muốn, dữ liệu đã có, địa điểm, tên đơn vị, tên dự án. **Không gửi** tên người liên hệ, email, số điện thoại. `raw_requirement_text` = đúng đoạn đã gửi AI.

**Phản hồi** (`ExtractionResponse`): `decision` (`HIGH` / `NEED_CONFIRM` / `INSUFFICIENT`), `decisionMessage`, `recommendationAllowed`, `outOfScope` (true khi `notes` bắt đầu bằng "Nhu cầu ngoài phạm vi dịch vụ hiện có:", nên hiện nổi bật), các mảng mã kèm tên, `availableInputCodes` (không lưu DB, dùng cho Validation Engine Tuần 3), `warnings`, `previousErrors`, `layer`.

## 4. Quy tắc lưu dữ liệu

| Dữ liệu | Nơi lưu |
|---|---|
| Mục tiêu, hiện trường, ngành, chủ đề | `requirement_tags` (thay toàn bộ mỗi lần trích xuất/sửa) |
| Đầu ra kỳ vọng | `requirement_expected_outputs` có `data_item_id`, `priority = NORMAL`. Dòng khách nhập tay (không có `data_item_id`) được giữ nguyên |
| Phương pháp | `extraction_method = AI` cho cả 3 lớp; `MANUAL` là mặc định khi chưa chạy AI |
| Độ tin cậy | `extraction_confidence = confidence.overall` (0–1) |
| `available_input_codes`, `notes`, `warnings`, lớp đã chạy | Chỉ trả về response và ghi log |
| Điểm D4 | `service_recommendations`; tín hiệu khách không nêu lưu `0` (cột NOT NULL) |

Requirement đã `CONFIRMED` thì không trích xuất lại / sửa mã được (lỗi 1403). Chạy gợi ý lại sẽ thay kết quả cũ nhưng giữ cờ `accepted` của dịch vụ còn trong shortlist.

## 5. D4 chấm điểm

Theo `ai/matching_rules.json` của SV3 (đổi số không cần sửa code, khởi động lại app):
trọng số mục tiêu 35 · đầu ra 25 · hiện trường 20 · ngành 12 · chủ đề 8; tín hiệu khách không nêu bị bỏ khỏi phép tính;
hiển thị dịch vụ `match_score ≥ 40`, tối đa 8; chỉ xét dịch vụ `active` và không `ARCHIVED`.

> **Lưu ý:** `GASCOLAE_Spec_Service_Recommendation.md` ghi trọng số 30/25/20/15/10 và ngưỡng 30. Backend theo D4 của SV3
> vì tiêu chí nghiệm thu đòi kết quả khớp `score.py`. Nếu nhóm chốt lại theo spec cũ, chỉ cần sửa `matching_rules.json`.

Phiếu không có mã mục tiêu → không chạy gợi ý, không sinh dòng nào (`ran = false`).

## 6. Kiểm thử nghiệm thu (BAN_GIAO_SV1 mục 10)

`./mvnw test` chạy các test sau, đáp án sinh từ bản Python của SV3 (`src/test/resources/sv3_reference/gen_reference.py`):

| Tiêu chí | Test | Phạm vi |
|---|---|---|
| 1. Hậu kiểm khớp Python | `ExtractionPostCheckerTest` | gộp trùng, loại mã lạ, câu cảnh báo giống hệt |
| 2. Chấm điểm khớp `score.py` (lệch ≤ 0.01, shortlist + thứ tự giống) | `RecommendationScorerTest` | 64 phiếu: 31 bảng từ khóa, 30 kết quả Gemini thật, 3 kịch bản; so cả câu lý do |
| 3. Ngưỡng đúng | `ExtractionPostCheckerTest`, `KeywordFallbackExtractorTest` | ngoài phạm vi → INSUFFICIENT; bảng từ khóa → luôn NEED_CONFIRM |
| 4. Dự phòng đúng | `ExtractionPipelineTest` | Gemini giả: 503 → thử lại → đổi model; 400 → bảng từ khóa; không key / mất mạng → không lỗi 500 |
| 5. Bảo mật | `ExtractionPipelineTest` | key bị che trong mọi lỗi |
| Bảng từ khóa khớp Python | `KeywordFallbackExtractorTest` | 30 câu + form mẫu, cả diện tích |
| Dựng input form khớp Python | `FormInputBuilderTest` | 6 form, không lọt thông tin cá nhân |

Gọi Gemini thật: chạy app với key, `GET /api/ai/status?ping=true`, rồi thử 3 câu trong `BAN_GIAO_SV1.md` mục 3 bằng `POST /api/ai/extract`.

## 7. Cập nhật khi SV3 đổi cấu hình

Thay file trong `src/main/resources/ai/` (prompt, schema, `keywords.json` = `KEYWORDS` trong `keywords.py`, `matching_rules.json`),
chạy lại `gen_reference.py` để sinh đáp án mới vào `src/test/resources/sv3_reference/`, rồi `./mvnw test`.
Không sửa prompt dựa trên lỗi của bộ 10 câu holdout.
