package backend.exception.messaging;

import backend.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MessagingErrorCode implements ErrorCode {

    MESSAGE_DISPATCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Kafka 메시지 채널 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    MessagingErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
