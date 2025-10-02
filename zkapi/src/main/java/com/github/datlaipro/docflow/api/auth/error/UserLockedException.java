package com.github.datlaipro.docflow.api.auth.error;

import com.github.datlaipro.docflow.api.shared.error.*;

public class UserLockedException extends AppException {
    public UserLockedException() {
        super(ErrorCode.USER_LOCKED, "User account is locked");
    }
}
