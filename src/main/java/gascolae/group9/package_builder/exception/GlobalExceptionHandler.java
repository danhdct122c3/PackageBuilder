package gascolae.group9.package_builder.exception;

import gascolae.group9.package_builder.dto.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static gascolae.group9.package_builder.exception.ErrorCode.UNCATEGORIZED_EXCEPTION;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<APIResponse> runTimeExceptionHandler(Exception e) {
        log.error("Exception: ", e);
        APIResponse apiResponse= APIResponse.builder()
                .code(UNCATEGORIZED_EXCEPTION.getCode())
                .message(UNCATEGORIZED_EXCEPTION.getMessage())
                .build();
        return ResponseEntity.status(UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<APIResponse> appExceptionHandler(AppException e) {
//    log.error("AppException: ", e);
    ErrorCode errorCode = e.getErrorCode();
        APIResponse apiResponse = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }
}
