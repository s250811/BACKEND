package backend.infrastructure.adapter.in.common;

import lombok.Getter;

@Getter
public class ApiResponseDto<T>{

    private static final String SUCCESS_STATUS = "success";
    private static final String FAIL_STATUS = "fail";
    private static final String ERROR_STATUS = "error";

    private String Status;
    private T data;
    private String message;

    public ApiResponseDto(String status, T data, String message) {
        this.Status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponseDto<T> createSuccess(T data, String message) {
        return new ApiResponseDto<>(SUCCESS_STATUS, data, message);
    }

    public static <T> ApiResponseDto<T> createSuccessNoContent(String message) {
        return new ApiResponseDto<>(SUCCESS_STATUS, null, message);
    }

    public static <T> ApiResponseDto<T> createFail(String message) {
        return new ApiResponseDto<>(FAIL_STATUS, null, message);
    }

    public static <T> ApiResponseDto<T> createError(String message) {
        return new ApiResponseDto<>(ERROR_STATUS, null, message);
    }
}

