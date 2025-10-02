package com.github.datlaipro.docflow.api.shared.error;


public enum ErrorCode {
    INVALID_CREDENTIALS(401),
    USER_INACTIVE(403),
    USER_LOCKED(423),
    TOO_MANY_ATTEMPTS(429),
    PASSWORD_EXPIRED(403),

    VALIDATION_ERROR(400),
    NOT_FOUND(404),
    CONFLICT(409),
    FORBIDDEN(403),

    DATABASE_ERROR(500),
    SERVER_ERROR(500);

    public final int httpStatus;
    ErrorCode(int s){ this.httpStatus = s; }
}
