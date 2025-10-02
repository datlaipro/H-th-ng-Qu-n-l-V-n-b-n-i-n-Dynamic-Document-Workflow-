package com.github.datlaipro.docflow.api.shared.error;


public class AppException extends RuntimeException {
    private final ErrorCode code;
    private final int httpStatus;
    private final Integer retryAfterSeconds; // chỉ dùng cho 429

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = code.httpStatus;
        this.retryAfterSeconds = null;
    }
    public AppException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = code.httpStatus;
        this.retryAfterSeconds = null;
    }
    protected AppException(ErrorCode code, String message, Integer retryAfterSeconds) {
        super(message);
        this.code = code;
        this.httpStatus = code.httpStatus;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ErrorCode getCode(){ return code; }
    public int getHttpStatus(){ return httpStatus; }
    public Integer getRetryAfterSeconds(){ return retryAfterSeconds; }
}
