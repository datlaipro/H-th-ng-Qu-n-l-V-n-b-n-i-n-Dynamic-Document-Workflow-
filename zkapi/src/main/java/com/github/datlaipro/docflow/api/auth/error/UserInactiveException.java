package com.github.datlaipro.docflow.api.auth.error;

import com.github.datlaipro.docflow.api.shared.error.*;

public class UserInactiveException extends AppException {
    public UserInactiveException() {
        super(ErrorCode.USER_INACTIVE, "User is inactive");
    }
}
