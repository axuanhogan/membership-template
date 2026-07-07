package com.axuanhogan.membership.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignInActivateRequest(
    @NotBlank(message = "Pre-Auth Token 不能為空")
    String preAuthToken,

    @NotBlank(message = "OTP 驗證碼不能為空")
    String otpCode
) {}
