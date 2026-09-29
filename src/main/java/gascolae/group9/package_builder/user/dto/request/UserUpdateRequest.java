package gascolae.group9.package_builder.user.dto.request;

import gascolae.group9.package_builder.exception.ErrorCode;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data // tạo getter, setter, toString, equals, hashCode
@Builder //cho phép sử dụng setter trên  dòng ko cần tạo 1 object rồi mới set
@NoArgsConstructor //tạo constructor ko tham số
@AllArgsConstructor //tạo constructor có tham số
@FieldDefaults(level = AccessLevel.PRIVATE) //gán AccessLevel.PRIVATE cho tất cả các field
public class UserUpdateRequest {
    @Email(message = "EMAIL_INVALID")
    String email;
    String fullName;
}
