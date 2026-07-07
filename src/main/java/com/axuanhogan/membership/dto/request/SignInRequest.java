package com.axuanhogan.membership.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
    @NotBlank(message = "Email 欄位不能為空")
    @Email(message = "Email 格式不正確")
    String email,

    @NotBlank(message = "密碼欄位不能為空")
    String password
) {}
