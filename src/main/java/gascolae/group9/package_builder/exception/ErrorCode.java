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
    INVALID_STATUS(1205, "Trạng thái không hợp lệ", HttpStatus.BAD_REQUEST), ;
    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.message = message;
        this.code = code;
        this.statusCode = statusCode;
    }
}
