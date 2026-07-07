package com.axuanhogan.membership.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignUpActivateRequest(
    @NotBlank(message = "開通 Token 欄位不能為空")
    String token
) {}
