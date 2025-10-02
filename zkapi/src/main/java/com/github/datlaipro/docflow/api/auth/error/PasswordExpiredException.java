package com.github.datlaipro.docflow.api.auth.error;

import com.github.datlaipro.docflow.api.shared.error.*;

public class PasswordExpiredException extends AppException {
    public PasswordExpiredException() {
        super(ErrorCode.PASSWORD_EXPIRED, "Password expired. Please reset your password.");
    }
}
