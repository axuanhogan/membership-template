package com.axuanhogan.membership.controller.v1;

import com.axuanhogan.membership.dao.User;
import com.axuanhogan.membership.dto.response.ApiResponse;
import com.axuanhogan.membership.dto.response.UserLastSignInResponse;
import com.axuanhogan.membership.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.ZoneId;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "會員管理模組", description = "提供獲取已登入會員個人資料等 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/last-sign-in")
    @Operation(summary = "查詢當前登入會員之最後登入時間", description = "系統將解析 Token 自動獲取當前登入者帳號，並回傳其最後登入時間。", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<UserLastSignInResponse>> getMyLastSignIn() {
        // 從 Spring Security 上下文取得經認證之 Email
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userService.findByEmail(email);

        String lastSignInStr = user.getLastSignInAt() != null ? user.getLastSignInAt().withZoneSameInstant(ZoneId.of("UTC+8")).toString() : "查無登入紀錄";

        String message = "查詢成功";
        UserLastSignInResponse response = new UserLastSignInResponse(
                user.getEmail(),
                lastSignInStr);
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
