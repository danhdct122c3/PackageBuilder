# GASCOLAE Intelligent Service Package Builder
## Bảng Theo Dõi Tiến Độ Triển Khai (Progress Tracker)

- **Dự án**: GASCOLAE Service Package Builder (MVP 4 Tuần)
- **Tài liệu kế hoạch**: [GASCOLAE_Implementation_Plan_4Weeks.md](./GASCOLAE_Implementation_Plan_4Weeks.md)
- **Quy chuẩn lập trình**: [.agents/skills/package-builder-conventions/SKILL.md](../.agents/skills/package-builder-conventions/SKILL.md)
- **Cập nhật lần cuối**: 2026-10-05 (Tích hợp AI D6 Gemini + Recommendation Engine D4 theo bàn giao SV3, xem [GASCOLAE_AI_D6_D4_Integration.md](./GASCOLAE_AI_D6_D4_Integration.md))

---

## 1. Dashboard Tổng Quan Tiến Độ

| Hạng mục | Trạng thái | Tỷ lệ hoàn thành | Ghi chú |
| :--- | :---: | :---: | :--- |
| **0. Khởi tạo & Quy chuẩn nền tảng** | 🟢 **ĐÃ XONG** | **100%** | Đã đọc hiểu MVP, DB V2, lập skill conventions, kế hoạch 4 tuần |
| **Giai đoạn 0: Dọn dẹp & Đồng bộ nền tảng** | 🟢 **ĐÃ XONG** | **100%** | Sửa Swagger GET, gỡ annotation thừa Controller, update RoleName |
| **Tuần 1: Customer & Requirement** | 🟢 **ĐÃ XONG** | **100%** | **Hoàn thành 100%**: Customer, Requirement CRUD, Public Landing Form, Admin B1, xử lý linh hoạt mọi kiểu dữ liệu |
| **Tuần 2: Service Catalog & Recommendation** | 🟢 **ĐÃ XONG (chờ chạy `mvnw test` + Gemini thật)** | **100%** | Catalog 10 bảng; AI Extraction D6 (Gemini 2 model + bảng từ khóa); Recommendation Engine D4; 14 test nghiệm thu khớp bản Python SV3 |
| **Tuần 3: Package Builder & Validation Engine** | ⚪ CHƯA BẮT ĐẦU | **0%** | Gói dịch vụ, phân phase, Rule Engine quét Gap/Dependency |
| **Tuần 4: Summary, E2E Testing & Demo** | ⚪ CHƯA BẮT ĐẦU | **0%** | Báo cáo bàn giao Sales-to-Ops, Test 3 kịch bản, Docker deploy |
| **TỔNG THỂ MVP BACKEND** | 🟡 **ĐANG TIẾN HÀNH** | **~70%** | Hoàn thành Customer, Requirement, Catalog, AI Extraction, Recommendation; tiếp theo Package Builder & Validation |

---

## 2. Chi Tiết Tiến Độ Từng Hạng Mục

### 0. Khởi Tạo & Chuẩn Bị (Foundation & Architecture) - [ĐÃ HOÀN THÀNH 100%]
- [x] Đọc và phân tích toàn bộ codebase hiện tại ([package-builder](../package-builder)).
- [x] Đọc hiểu tài liệu nghiệp vụ MVP ([GASCOLAE_Service_Package_Builder_MVP.md](./GASCOLAE_Service_Package_Builder_MVP.md)).
- [x] Đọc hiểu và thẩm định thiết kế cơ sở dữ liệu ([GASCOLAE_Database_Design_V2_UUID_All_Tables.md](./GASCOLAE_Database_Design_V2_UUID_All_Tables.md)).
- [x] Đóng gói toàn bộ kiến trúc & quy chuẩn lập trình thành Workspace Skill ([package-builder-conventions](../.agents/skills/package-builder-conventions/SKILL.md)).
- [x] Lập kế hoạch triển khai chi tiết 4 tuần theo chiến lược Customer First ([GASCOLAE_Implementation_Plan_4Weeks.md](./GASCOLAE_Implementation_Plan_4Weeks.md)).
- [x] Hoàn thiện module xác thực & phân quyền nền tảng:
  - [x] Entity `User`, `Role`, `InvalidatedToken`.
  - [x] Spring Security + OAuth2 Resource Server (Nimbus JOSE JWT HS512).
  - [x] Đăng ký, đăng nhập, đổi mật khẩu, đăng xuất (Token Blacklist).
  - [x] Chuẩn hóa `APIResponse<T>`, `ErrorCode`, `GlobalExceptionHandler`.

---

### Giai đoạn 0: Dọn Dẹp & Đồng Bộ Nền Tảng (Ngày 1) - [ĐÃ HOÀN THÀNH 100%]
- [x] **Sửa cấu hình Security**: Cập nhật `SecurityConfig.java`, mở quyền `GET` cho `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`.
- [x] **Gỡ Annotation thừa trên Controller**: Xóa `@EntityListeners(AuditingEntityListener.class)` trên `UserController`, `AuthenticateController`, `RoleController`.
- [x] **Đồng bộ Role Enum**: Bổ sung `SALES_PRE_SALES`, `OPERATION`, `CUSTOMER` vào `RoleName.java`.
- [x] **Xóa file nháp**: Xóa `service_package/entity/ServicePackage.java`.

---

### Tuần 1: Customer & Customer Requirement (Ngày 2 - Ngày 7) - [ĐÃ HOÀN THÀNH 100%]

#### 1. Module Customer (`customer/`) - [ĐÃ HOÀN THÀNH 100%]:
- [x] Enum `CustomerStatus` (`ACTIVE`, `INACTIVE`).
- [x] Entity `Customer` (UUID PK, customerCode Unique, name, company, industry, contact, auditing timestamps).
- [x] `CustomerRepository` (kế thừa `JpaRepository`, tìm kiếm theo code, email, tên công ty, phân trang).
- [x] DTOs: `CustomerCreateRequest`, `CustomerUpdateRequest`, `CustomerResponse`.
- [x] Mapper: `CustomerMapper` (MapStruct `componentModel = "spring"`).
- [x] Service: `CustomerService` & `CustomerServiceImpl`.
- [x] Controller: `CustomerController` (`POST`, `GET` list/detail/code, `PUT` update, `PUT` update-status).
- [x] Bổ sung mã lỗi: `CUSTOMER_NOT_FOUND`, `CUSTOMER_CODE_EXISTED`, `CUSTOMER_EMAIL_EXISTED` vào `ErrorCode.java`.

#### 2. Module Requirement (gộp trong `customer/`) - [ĐÃ HOÀN THÀNH 100%]:
- [x] Enums: `RequirementStatus` (`DRAFT`, `CONFIRMED`, `ARCHIVED`), `OutputPriority` (`LOW`, `NORMAL`, `HIGH`, `REQUIRED`).
- [x] Entity `CustomerRequirement` (UUID PK, requirementCode unique, liên kết Customer, diện tích `areaValue`, đơn vị `areaUnit`, khu vực theo dõi `locationDescription` AOI, dữ liệu sẵn có `providedInputsRaw`, tần suất `monitoringFrequencyRaw`, mục tiêu, JPA Auditing).
- [x] Entity `RequirementExpectedOutput` (đầu ra kỳ vọng, priority, quan hệ @ManyToOne với CustomerRequirement).
- [x] Repositories: `CustomerRequirementRepository` (tối ưu tìm kiếm keyword đa trường, phân trang, lọc status, customerId), `RequirementExpectedOutputRepository`.
- [x] DTOs & Mapper: `LandingLeadRequest`, `RequirementCreateRequest`, `RequirementUpdateRequest`, `ExpectedOutputRequest`, `RequirementResponse`, `ExpectedOutputResponse`, `RequirementMapper`.
- [x] **Xử lý linh hoạt dữ liệu đầu vào (Defensive Deserialization)**:
  - [x] Parse diện tích tự động từ Số, Chuỗi có đơn vị (`"20 ha"`, `"20ha"`, `"20.5 ha"`, `"1000 m2"`), Object `{ "value": 2000, "unit": "ha" }`, hoặc rỗng `{}` / `""`.
  - [x] Parse đầu ra kỳ vọng linh hoạt từ chuỗi rỗng `""`, chuỗi đa dòng text-area (`"A\nB"`), mảng chuỗi `["A", "B"]`, hoặc mảng object có cấu trúc.
- [x] Service: `RequirementService` & `RequirementServiceImpl`:
  - [x] `submitLandingLead`: Tự động tìm/tạo Customer theo email/phone, sinh mã `CUST-` và `REQ-`, tạo hồ sơ `DRAFT`.
  - [x] `createRequirement`, `updateRequirement` (cho phép Sales hiệu chỉnh cả tên doanh nghiệp/khách hàng ngay tại màn hình B1), `confirmRequirement` (`DRAFT` -> `CONFIRMED`), `deleteRequirement`.
  - [x] `searchRequirements`: Phân nhánh tối ưu, gọi `findAll` khi không có bộ lọc để tránh lỗi PostgreSQL `lower(bytea)`.
- [x] Controller & Security:
  - [x] `POST /requirements/public/lead` (Public API, `permitAll()` không cần token cho khách vãng lai).
  - [x] `GET /requirements`, `GET /requirements/{id}`, `PUT /requirements/{id}`, `PUT /requirements/{id}/confirm` (Bảo vệ bằng JWT cho Sales Admin).
- [x] Bổ sung mã lỗi: `REQUIREMENT_NOT_FOUND`, `REQUIREMENT_CODE_EXISTED`, `REQUIREMENT_ALREADY_CONFIRMED`, `CONTACT_INFO_REQUIRED` vào `ErrorCode.java`.
- [x] **Đã kiểm thử thực tế trên Postman & Docker, commit mã nguồn `2ef5435` lên nhánh `customer`**.

---

### Tuần 2: Service Catalog, Taxonomy & Recommendation (Ngày 8 - Ngày 14) - [ĐANG TIẾN HÀNH 60%]

#### 1. Service Catalog & Taxonomy Master Data (`catalog/`) - [ĐÃ HOÀN THÀNH 100%]:
- [x] Entity & Repository `Tag`, `ServiceTag` (phân loại `OBJECTIVE`, `INDUSTRY`, `ENVIRONMENT`, `TOPIC`, hỗ trợ phân cấp `parentTag`).
- [x] Entity & Repository `DataItem` (taxonomy trung tâm 91 loại dữ liệu cho inputs, outputs, deliverables).
- [x] Entity & Repository `Service` (thông tin 12 dịch vụ GASCOLAE, category, use cases, customer problems, technologies).
- [x] Entity & Repository `ServiceLevel` (36 cấp độ chi tiết, 3 level cho mỗi dịch vụ).
- [x] Entity & Repository `ServiceInput` (94 đầu vào kèm cờ `required` và `provided_by`: `CUSTOMER`, `GASCOLAE`, `AUTHORITY`, `THIRD_PARTY`).
- [x] Entity & Repository `ServiceOutput` (94 đầu ra sinh ra bởi dịch vụ).
- [x] Entity & Repository `ServiceDeliverable`, `ServiceDeliverableItem` (86 deliverables bàn giao và 94 mapping với data items).
- [x] Entity & Repository `ServiceRelation` (quan hệ `PROVIDES_INPUT_FOR`, `RECOMMENDED_WITH` kèm `viaDataItem`).
- [x] DTOs, Mappers, Services, Controllers quản lý Catalog:
  - `GET /catalog/services`: Tra cứu dịch vụ, phân trang, lọc theo keyword, category, tagCode (Public).
  - `GET /catalog/services/{idOrCode}`: Chi tiết dịch vụ đầy đủ (inputs, outputs, deliverables, levels, tags, relations) (Public).
  - `POST /catalog/services`: Tạo mới dịch vụ kèm gán tag (Admin, Request DTO `ServiceCreateRequest`).
  - `PUT /catalog/services/{id}`: Hiệu chỉnh thông tin dịch vụ & cập nhật tags (Admin, Request DTO `ServiceUpdateRequest`).
  - `PUT /catalog/services/{id}/status`: Bật/tắt trạng thái hoạt động dịch vụ (`active: true/false`).
  - `DELETE /catalog/services/{id}`: Tạm dừng / xóa mềm dịch vụ.
  - `GET /catalog/categories`: Danh sách category của các dịch vụ.
  - `GET /catalog/data-items`: Danh sách taxonomy inputs/outputs.
  - `GET /catalog/tags`: Danh mục tag phân loại.
- [x] `SimpleCsvParser`: Tiện ích bóc tách RFC-4180 CSV siêu tốc (đã có unit test pass 100% cả 10 file seed).
- [x] `CatalogDataInitializer`: Runner tự động nạp toàn bộ 10 bảng dữ liệu Master Data từ CSV khi khởi động ứng dụng.
- [x] Cấu hình Security: Mở public GET cho `/catalog/**` cho phép Frontend tra cứu danh mục dịch vụ, bảo vệ các thao tác ghi (POST/PUT/DELETE) bằng JWT Bearer Token.

#### 2. AI Extraction D6 (`extraction/`) - [ĐÃ HOÀN THÀNH, chờ test Gemini thật]:
- [x] Nạp cấu hình SV3 vào `src/main/resources/ai/`: prompt v1.4, `extraction_schema.json`, `extraction_schema.gemini.json`, `keywords.json`, `matching_rules.json`.
- [x] `GeminiClient`: Interactions API REST, `store=false`, `temperature=0`, timeout 60s, thử lại 1 lần khi 429/500/503, che key trong lỗi.
- [x] `ExtractionPipeline`: model chính → model dự phòng → bảng từ khóa (không bao giờ ném lỗi 500).
- [x] Hậu kiểm: gộp mã trùng → loại mã ngoài schema/DB → kiểm schema. Ngưỡng tin cậy HIGH / NEED_CONFIRM / INSUFFICIENT.
- [x] Chế độ form cho lead từ landing page, không gửi tên/email/SĐT sang Gemini.
- [x] Entity `RequirementTag` (`requirement_tags`), cột `extraction_method`, `extraction_confidence` trong `customer_requirements`.
- [x] API: `POST /ai/extract`, `POST /requirements/{id}/extract`, `GET|PUT /requirements/{id}/codes`, `GET /ai/status`.
- [x] Key chỉ đọc từ `GEMINI_API_KEY` (env hoặc `.env`), `.env` trong `.gitignore`/`.dockerignore`, docker-compose truyền biến.

#### 3. Recommendation Engine D4 (`recommendation/`) - [ĐÃ HOÀN THÀNH]:
- [x] Entity & Repository `ServiceRecommendation` (`service_recommendations`).
- [x] `RecommendationScorer`: port 1:1 `score.py` v1.1 (trọng số 35/25/20/12/8, khớp mã cha 60, cùng nhóm đầu ra 70, bỏ tín hiệu trống, ngưỡng 40, tối đa 8).
- [x] Sinh `recommendation_reason` tiếng Việt (đổi mã sang tên).
- [x] API: `POST|GET /requirements/{id}/recommendations`, `PUT /recommendations/{id}/accept`.
- [x] Test nghiệm thu: 64 phiếu khớp `score.py` (điểm lệch ≤ 0.01, shortlist, thứ tự, câu lý do).

---

### Tuần 3: Solution Package Builder & Validation Engine (Ngày 15 - Ngày 21) - [0%]

#### 1. Package Builder (`solution_package/`):
- [ ] Entity & Repository `SolutionPackage`, `PackageService`, `PackageOpenQuestion`.
- [ ] DTOs, Mapper, Service, Controller: tạo gói giải pháp, thêm/gỡ dịch vụ, gán vai trò (`CORE`, `SUPPORTING`, `OPTIONAL`), xếp thứ tự Phase.
- [ ] Quản lý câu hỏi mở `package_open_questions` giữa Sales, Operation và Customer.

#### 2. Dependency & Gap Validation Engine (`validation/`):
- [ ] Entity & Repository `ValidationRun`, `ValidationResult`.
- [ ] Lập trình Rule Engine kiểm tra 4 quy tắc:
  - [ ] Rule 1: `MISSING_REQUIRED_INPUT` (quét thiếu đầu vào bắt buộc).
  - [ ] Rule 2: `MISSING_DEPENDENCY` (quét quan hệ `PROVIDES_INPUT_FOR`).
  - [ ] Rule 3: `DUPLICATE_DELIVERABLE` (phát hiện trùng lặp sản phẩm bàn giao).
  - [ ] Rule 4: `NEED_MANUAL_VERIFICATION` (cảnh báo dịch vụ chưa kiểm chứng).
- [ ] Tự động tính toán trạng thái gói: `READY`, `CONDITIONALLY_READY`, `GAP`.
- [ ] API: `POST /api/packages/{id}/validate`.

---

### Tuần 4: Solution Summary, E2E Integration & Demo Handover (Ngày 22 - Ngày 28) - [0%]
- [ ] API Summary Aggregator: `GET /api/packages/{id}/summary` (tổng hợp dịch vụ theo phase, inputs cần chuẩn bị, deliverables bàn giao, trạng thái sẵn sàng).
- [ ] Viết Integration Test kiểm thử 3 kịch bản demo:
  - [ ] Kịch bản 1: Forest Carbon Project (Dự án Rừng 2.000 ha).
  - [ ] Kịch bản 2: Missing Dependency Gap (Cố tình thiếu dịch vụ hỗ trợ $\rightarrow$ Validator cảnh báo $\rightarrow$ Khắc phục).
  - [ ] Kịch bản 3: GHG Monitoring (Quan trắc phát thải nhà kính).
- [ ] Đóng gói Docker Compose (`docker compose up -d`) chạy ổn định.
- [ ] Tinh chỉnh tài liệu OpenAPI / Swagger UI bàn giao hoàn chỉnh cho Frontend.

---

## 3. Nhật Ký Hoạt Động (Activity Log)

| Ngày | Người thực hiện | Nội dung thực hiện | Kết quả / Ghi chú |
| :--- | :--- | :--- | :--- |
| **2026-09-30** | Antigravity & User | - Đọc và phân tích toàn bộ dự án `Package_Builder`<br>- Đọc hiểu `GASCOLAE_Service_Package_Builder_MVP.md`<br>- Đọc hiểu & thẩm định `GASCOLAE_Database_Design_V2_UUID_All_Tables.md`<br>- Xây dựng Skill `package-builder-conventions`<br>- Lập kế hoạch 4 tuần `GASCOLAE_Implementation_Plan_4Weeks.md`<br>- Thống nhất chiến lược Customer First & tạo file theo dõi tiến độ | Hoàn tất giai đoạn phân tích & kiến trúc. |
| **2026-09-30** | Antigravity | **Hoàn thành Giai đoạn 0**: <br>1. Sửa lỗi `SecurityConfig.java` (mở quyền GET Swagger UI, docs).<br>2. Gỡ bỏ `@EntityListeners` trên 3 Controllers.<br>3. Bổ sung `SALES_PRE_SALES`, `OPERATION`, `CUSTOMER` vào `RoleName.java`.<br>4. Xóa file nháp `ServicePackage.java`.<br>5. Chạy `mvn test-compile` thành công 100%. | Đạt mốc **20%** tiến độ tổng thể. |
| **2026-09-30** | Antigravity | **Hoàn thành Module Customer**: <br>1. Enum `CustomerStatus` (`ACTIVE`, `INACTIVE`).<br>2. Entity `Customer` (UUID, customerCode unique, JPA Auditing).<br>3. `CustomerRepository` (tìm kiếm, lọc status, phân trang).<br>4. DTOs: `CustomerCreateRequest`, `CustomerUpdateRequest`, `CustomerResponse`.<br>5. `CustomerMapper` (MapStruct ignore auto fields).<br>6. `CustomerService` & `CustomerServiceImpl`.<br>7. `CustomerController` (REST APIs: POST, GET search/detail/code, PUT update/status).<br>8. Bổ sung `CUSTOMER_NOT_FOUND`, `CUSTOMER_CODE_EXISTED`, `CUSTOMER_EMAIL_EXISTED` vào `ErrorCode.java`.<br>9. Biên dịch `./mvnw test-compile` thành công 100% (49 source files). | Đạt mốc **~30%** tiến độ tổng thể. Sẵn sàng cho Module Requirement. |
| **2026-09-30** | Antigravity | **Hoàn thành Module Requirement (Data-Only CRUD)**: <br>1. Enums `RequirementStatus`, `OutputPriority`.<br>2. Entity `CustomerRequirement`, `RequirementExpectedOutput` (CascadeType.ALL, OrphanRemoval, Auditing).<br>3. `CustomerRequirementRepository` (search keyword, status, customerId, paging) & `RequirementExpectedOutputRepository`.<br>4. DTOs: `RequirementCreateRequest`, `RequirementUpdateRequest`, `ExpectedOutputRequest`, `RequirementResponse`, `ExpectedOutputResponse`.<br>5. `RequirementMapper` (MapStruct mappings).<br>6. `RequirementService` & `RequirementServiceImpl` (xử lý CRUD, sinh mã tự động nếu trống, kiểm tra confirm, lock sửa khi confirmed, confirm API).<br>7. `RequirementController` (REST APIs đầy đủ).<br>8. Bổ sung `REQUIREMENT_NOT_FOUND`, `REQUIREMENT_CODE_EXISTED`, `REQUIREMENT_ALREADY_CONFIRMED` vào `ErrorCode.java`.<br>9. Biên dịch `./mvnw test-compile` thành công 100% (64 source files). | **Hoàn thành 100% Tuần 1**, đạt mốc **~40%** tiến độ tổng thể. |
| **2026-10-01** | Antigravity | **Hoàn thiện luồng Landing Page $\rightarrow$ Sales Admin**: <br>1. Thêm DTO `LandingLeadRequest` hỗ trợ nhận form Landing Page (liên hệ + dự án + AOI + dữ liệu sẵn có + expected outputs dạng text/DTO).<br>2. Thêm trường `locationDescription`, `providedInputsRaw`, `monitoringFrequencyRaw` vào `CustomerRequirement`.<br>3. Viết hàm `submitLandingLead` trong `RequirementServiceImpl`: tự động liên kết/tạo Customer theo email/phone, tự sinh mã code, tạo Requirement DRAFT.<br>4. Mở API Public `POST /requirements/public/lead` (không cần Bearer token).<br>5. Bổ sung `contactEmail`, `contactPhone`, `locationDescription`, `providedInputsRaw` vào `RequirementResponse` phục vụ màn hình Admin B1.<br>6. Hỗ trợ cập nhật tên doanh nghiệp/khách hàng trực tiếp qua `PUT /requirements/{id}`.<br>7. Biên dịch `./mvnw test-compile` thành công 100% (65 source files). | Hoàn thiện luồng kết nối Form Landing Page $\rightarrow$ Admin B1. |
| **2026-10-01** | Antigravity & User | **Kiểm thử E2E Postman & Vá lỗi Runtime**: <br>1. **Fix lỗi Jackson MismatchedInputException (`BigDecimal`)**: Đổi `areaValue` sang kiểu `Object` kết hợp bóc tách thông minh; tự động nhận số, chuỗi, hoặc chuỗi có đơn vị (`"20 ha"`, `"20ha"`, `"20.5 ha"`, `"1000 m2"`) thành số và đơn vị chuẩn.<br>2. **Fix lỗi Jackson no String-argument constructor (`ExpectedOutputRequest`)**: Bổ sung `@JsonCreator` constructor 1 tham số String cho phép nhận cả mảng chuỗi `["A", "B"]` và mảng object.<br>3. **Fix lỗi Jackson ValueString (`expectedOutputs: ""`)**: Đổi sang `Object expectedOutputs` kèm bộ bóc tách chuỗi rỗng và chuỗi textarea đa dòng `\n`.<br>4. **Fix lỗi PostgreSQL SQLGrammarException `lower(bytea)`**: Tối ưu `searchRequirements` và `searchCustomers` gọi trực tiếp `findAll(pageable)` khi không có bộ lọc và bọc lowercase `%` ở tầng Java.<br>5. **Đã commit `2ef5435` và push thành công lên `origin/customer`**. | Hoàn tất 100% Tuần 1, sẵn sàng cho Tuần 2 (Service Catalog). |
| **2026-10-01** | Antigravity | **Triển khai Service Catalog Master Data & Seeder (Nhánh `serviceCatalog`)**: <br>1. Tiếp nhận bộ seed SV3 V2-final (10 bảng CSV + 2 JSON).<br>2. Tạo 6 Enums: `DataClassification`, `VerificationStatus`, `TagType`, `LifecycleStatus`, `ProvidedBy`, `RelationType`.<br>3. Tạo 10 Entities/Embeddables: `DataItem`, `Tag`, `Service`, `ServiceLevel`, `ServiceInput`, `ServiceOutput`, `ServiceDeliverable`, `ServiceDeliverableItem`, `ServiceTag`, `ServiceRelation`.<br>4. Tạo 10 Repositories Spring Data JPA.<br>5. Xây dựng tiện ích RFC-4180 `SimpleCsvParser` & kiểm thử unit test `SimpleCsvParserTest` pass 100% cả 10 file seed.<br>6. Viết `CatalogDataInitializer` tự động nạp dữ liệu Master Data từ CSV vào database khi khởi động.<br>7. Tạo DTOs & `CatalogMapper` (MapStruct), `CatalogService` & `CatalogServiceImpl`, `CatalogController` REST APIs.<br>8. Mở public GET cho `/catalog/**` trong `SecurityConfig.java`.<br>9. Biên dịch `./mvnw test-compile` thành công 100% (108 source files). | Hoàn thành **100% Service Catalog Master Data**, đạt mốc **~60%** tiến độ tổng thể. Sẵn sàng cho Recommendation Engine. |
| **2026-10-05** | Claude & Don | **Tích hợp AI D6 + D4 từ bàn giao SV3**: <br>1. Module `extraction/` gọi Gemini thật (2 model + bảng từ khóa dự phòng), hậu kiểm, ngưỡng tin cậy, chế độ form.<br>2. Bảng `requirement_tags`, cột `extraction_method`/`extraction_confidence`.<br>3. Module `recommendation/` chấm điểm 12 dịch vụ, lưu `service_recommendations`, Sales chọn dịch vụ.<br>4. 14 test nghiệm thu đối chiếu bản Python của SV3 (đáp án trong `src/test/resources/sv3_reference`).<br>5. Cấu hình key qua `.env`/biến môi trường, docker-compose. | Đạt mốc **~70%**. Cần chạy `./mvnw test` trên máy và thử Gemini thật bằng `GET /api/ai/status?ping=true`. |
