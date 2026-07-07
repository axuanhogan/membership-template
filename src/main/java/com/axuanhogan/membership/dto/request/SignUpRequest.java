package com.axuanhogan.membership.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank(message = "Email 欄位不能為空")
    @Email(message = "Email 格式不正確")
    String email,

    @NotBlank(message = "密碼欄位不能為空")
    @Size(min = 6, max = 50, message = "密碼長度必須在 6 至 50 個字元之間")
    String password
) {}
