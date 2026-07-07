package com.axuanhogan.membership.service;

import com.axuanhogan.membership.dao.OtpToken;
import com.axuanhogan.membership.dao.User;
import com.axuanhogan.membership.repository.OtpTokenRepository;
import com.axuanhogan.membership.repository.UserRepository;
import com.axuanhogan.membership.security.TokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       OtpTokenRepository otpTokenRepository,
                       TokenProvider tokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 會員註冊
     */
    public User signUp(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("此 Email 帳號已被註冊");
        }

        User user = new User(email, passwordEncoder.encode(rawPassword));
        user.setEnabled(false);
        user.setActivationToken(UUID.randomUUID().toString());
        user.setActivationTokenExpiresAt(ZonedDateTime.now().plusMinutes(30));

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    /**
     * 啟用會員帳號
     */
    public void activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("無效的開通連結"));

        if (user.isEnabled()) {
            return; // 已經開通，直接返回
        }

        if (user.getActivationTokenExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("開通連結已逾期，請重新註冊");
        }

        user.setEnabled(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiresAt(null);
        userRepository.save(user);
    }

    /**
     * 會員登入第一階段：密碼驗證與發送 2FA OTP
     * 回傳一個短效的 Pre-Auth Token
     */
    public String initiateSignIn(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("該帳號尚未開通，請先至 Email 點擊連結啟用");
        }

        // 產生 6 位數字 OTP 驗證碼
        String otpCode = generateOtpCode();

        // 儲存 OTP 驗證碼，效期 5 分鐘
        OtpToken otpToken = new OtpToken(email, otpCode, 5);
        otpTokenRepository.save(otpToken);

        return otpCode;
    }

    /**
     * 會員登入第二階段：驗證 Pre-Auth Token 與 OTP
     * 驗證成功後回傳正式的 Access Token 與登入前最後一次登入時間
     */
    public String verify2Fa(String preAuthToken, String otpCode) {
        // 驗證 Pre-Auth Token 是否合法
        if (!tokenProvider.validateToken(preAuthToken)) {
            throw new IllegalArgumentException("無效或已逾期的 Pre-Auth 憑證");
        }

        String tokenType = tokenProvider.getTokenTypeFromToken(preAuthToken);
        if (!"PRE_AUTH".equals(tokenType)) {
            throw new IllegalArgumentException("此憑證不支援兩階段驗證");
        }

        String email = tokenProvider.getEmailFromToken(preAuthToken);

        // 驗證 OTP
        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(email, otpCode)
                .orElseThrow(() -> new IllegalArgumentException("驗證碼錯誤或已失效"));

        if (otpToken.isExpired()) {
            throw new IllegalArgumentException("驗證碼已逾期，請重新登入發送");
        }

        // 標記 OTP 已使用
        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        return email;
    }

    private String generateOtpCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    // 用於回傳登入結果的 Record
    public record SignInResult(String accessToken, ZonedDateTime lastSignInAt) {}
}
