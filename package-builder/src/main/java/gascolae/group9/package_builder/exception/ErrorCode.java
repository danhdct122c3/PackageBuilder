package gascolae.group9.package_builder.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // 9xxx - System
    UNCATEGORIZED_EXCEPTION(9000, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // 10xx - Common validation and authentication
    INVALID_KEY(1001, "Khóa thông báo không hợp lệ", HttpStatus.BAD_REQUEST),
    NOT_NULL(1002, "Vui lòng điền vào tất cả các trường", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1003, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    PHONENUMBER_INVALID(1004, "Số điện thoại không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1010, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1011, "Bạn không có quyền truy cập", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1012, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    PASSWORD_NOT_MATCH(1013, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1014, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    USER_NOT_ACTIVE(1015, "Tài khoản của bạn chưa được kích hoạt", HttpStatus.FORBIDDEN),

    // 11xx - User
    USER_NOT_EXISTED(1101, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(1102, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1103, "Tên đăng nhập phải có ít nhất {min} ký tự", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1104, "Mật khẩu phải có ít nhất {min} ký tự", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1105, "Tuổi của bạn phải ít nhất {min}", HttpStatus.NOT_FOUND),
    USER_EMAIL_EXISTED(1106, "Email đã tồn tại trong hệ thống", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_EXISTED(1107, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    EMAIL_INVALID(1108, "Email không hợp lệ", HttpStatus.BAD_REQUEST),

    // 12xx - RoleName and permission
    INVALID_ROLE(1201, "Vai trò không hợp lệ", HttpStatus.BAD_REQUEST),
    ROLE_EXISTED(1202, "Vai trò đã tồn tại", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXISTED(1203, "Vai trò không tồn tại", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_EXISTED(1204, "Quyền không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_STATUS(1205, "Trạng thái không hợp lệ", HttpStatus.BAD_REQUEST),

    // 13xx - Customer
    CUSTOMER_NOT_FOUND(1301, "Không tìm thấy thông tin khách hàng", HttpStatus.NOT_FOUND),
    CUSTOMER_CODE_EXISTED(1302, "Mã khách hàng đã tồn tại", HttpStatus.BAD_REQUEST),
    CUSTOMER_EMAIL_EXISTED(1303, "Email khách hàng đã tồn tại trong hệ thống", HttpStatus.BAD_REQUEST),
    CONTACT_INFO_REQUIRED(1304, "Vui lòng cung cấp ít nhất Email hoặc Số điện thoại", HttpStatus.BAD_REQUEST),

    // 14xx - Requirement
    REQUIREMENT_NOT_FOUND(1401, "Không tìm thấy yêu cầu của khách hàng", HttpStatus.NOT_FOUND),
    REQUIREMENT_CODE_EXISTED(1402, "Mã yêu cầu dự án đã tồn tại", HttpStatus.BAD_REQUEST),
    REQUIREMENT_ALREADY_CONFIRMED(1403, "Yêu cầu đã được xác nhận, không thể chỉnh sửa", HttpStatus.BAD_REQUEST),

    // 15xx - Catalog
    SERVICE_NOT_FOUND(1501, "Dịch vụ không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    DATA_ITEM_NOT_FOUND(1502, "Dữ liệu taxonomy không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    TAG_NOT_FOUND(1503, "Tag không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    SERVICE_CODE_EXISTED(1504, "Mã dịch vụ đã tồn tại trong hệ thống", HttpStatus.BAD_REQUEST),

    // 16xx - AI Extraction (D6)
    EXTRACTION_INPUT_EMPTY(1601, "Chưa có câu nhu cầu hoặc trường mô tả nào để AI trích xuất", HttpStatus.BAD_REQUEST),

    // 17xx - Recommendation (D4)
    REQUIREMENT_NOT_CONFIRMED(1701, "Yêu cầu phải được Sales xác nhận trước khi chạy gợi ý dịch vụ", HttpStatus.BAD_REQUEST),
    RECOMMENDATION_NOT_FOUND(1702, "Không tìm thấy gợi ý dịch vụ", HttpStatus.NOT_FOUND),
    ;
    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.message = message;
        this.code = code;
        this.statusCode = statusCode;
    }
}
