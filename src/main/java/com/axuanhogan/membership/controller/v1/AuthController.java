package com.axuanhogan.membership.controller.v1;

import com.axuanhogan.membership.dao.User;
import com.axuanhogan.membership.dto.request.SignInActivateRequest;
import com.axuanhogan.membership.dto.request.SignInRequest;
import com.axuanhogan.membership.dto.request.SignUpRequest;
import com.axuanhogan.membership.dto.response.ApiResponse;
import com.axuanhogan.membership.dto.response.SignUpResponse;
import com.axuanhogan.membership.dto.response.SignInResponse;
import com.axuanhogan.membership.dto.response.SignInActivateResponse;
import com.axuanhogan.membership.security.TokenProvider;
import com.axuanhogan.membership.service.AuthService;
import com.axuanhogan.membership.service.MailService;
import com.axuanhogan.membership.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "會員驗證模組", description = "提供會員註冊、帳號開通、密碼驗證以及兩階段 OTP 驗證之 API")
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;
    private final TokenProvider tokenProvider;
    private final UserService userService;

    public AuthController(
            AuthService authService,
            MailService mailService,
            TokenProvider tokenProvider,
            UserService userService) {
        this.authService = authService;
        this.mailService = mailService;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @PostMapping("/sign-up")
    @Operation(summary = "會員註冊", description = "使用 Email 與密碼進行註冊。註冊成功後帳號為停用狀態，系統會寄送開通郵件。")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = authService.signUp(request.email(), request.password());

        String activationLink = "http://localhost:8080/v1/auth/sign-up/activate?token=" + user.getActivationToken();
        mailService.sendActivationEmail(user.getEmail(), activationLink);

        String message = "註冊成功，請查看信箱並點擊開通連結。";
        SignUpResponse response = new SignUpResponse(user.getEmail());
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/sign-up/activate")
    @Operation(summary = "帳號開通", description = "接收 Email 連結中的開通 Token，驗證無誤後啟用使用者帳號。")
    public ResponseEntity<ApiResponse<Void>> signUpActivate(@RequestParam("token") String token) {
        authService.activateAccount(token);

        String message = "帳號開通成功！現在可以開始登入。";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/sign-in")
    @Operation(summary = "會員登入 - 第一階段 (密碼驗證)", description = "驗證帳號與密碼是否正確，通過後會寄送一組 OTP 至使用者 Email，並回傳 preAuthToken。")
    public ResponseEntity<ApiResponse<SignInResponse>> signIn(@Valid @RequestBody SignInRequest request) {
        String email = request.email();
        String otpCode = authService.initiateSignIn(email, request.password());
        mailService.sendOtpEmail(email, otpCode);
        String preAuthToken = tokenProvider.generatePreAuthToken(email);

        String message = "第一階段驗證成功。請於信箱收取驗證碼(OTP)，並於 5 分鐘內完成驗證。";
        SignInResponse response = new SignInResponse(preAuthToken);
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PatchMapping("/sign-in")
    @Operation(summary = "會員登入 - 第二階段 (2FA 驗證)", description = "帶入第一階段的 preAuthToken 與信箱收到的 OTP 完成驗證。成功後回傳正式的 JWT Access Token。")
    public ResponseEntity<ApiResponse<SignInActivateResponse>> signInActivate(
            @Valid @RequestBody SignInActivateRequest request) {
        String email = authService.verify2Fa(request.preAuthToken(), request.otpCode());
        userService.updateLastSignInAt(email);
        String accessToken = tokenProvider.generateAccessToken(email);

        String message = "登入成功！";
        SignInActivateResponse response = new SignInActivateResponse(accessToken);
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
