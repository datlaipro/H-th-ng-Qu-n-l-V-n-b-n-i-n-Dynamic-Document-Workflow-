package com.github.datlaipro.docflow.api.auth.error;


import com.github.datlaipro.docflow.api.shared.error.*;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Email/username or password is incorrect");
    }
}
