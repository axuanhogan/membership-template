package com.axuanhogan.membership.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String MAILJET_URL = "https://api.mailjet.com/v3.1/send";

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    @Value("${mailjet.sender-email}")
    private String senderEmail;

    @Value("${mailjet.sender-name}")
    private String senderName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 發送開通帳號電子郵件
     */
    public void sendActivationEmail(String toEmail, String activationToken) {
        String activationLink = "http://localhost:8080/api/auth/activate?token=" + activationToken;
        log.info("[Test Helper] 帳號開通連結 (若 Mailjet 未設定或發送失敗，可直接複製此連結測試): {}", activationLink);

        String subject = "會員帳號啟用信";
        String htmlContent = String.format(
                "<h3>歡迎註冊！</h3>" +
                        "<p>請點擊下方連結以開通您的帳號：</p>" +
                        "<a href='%s' target='_blank'>%s</a>" +
                        "<p>此連結有效時間為 30 分鐘。</p>",
                activationLink, activationLink);

        sendEmailViaMailjet(toEmail, subject, htmlContent);
    }

    /**
     * 發送兩階段驗證 OTP 電子郵件
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("[Test Helper] 兩階段驗證 OTP (若 Mailjet 未設定或發送失敗，可使用此驗證碼): {}", otpCode);

        String subject = "登入兩階段驗證碼";
        String htmlContent = String.format(
                "<h3>您好，</h3>" +
                        "<p>您的登入驗證碼為：<strong style='font-size: 24px; color: #1e88e5;'>%s</strong></p>" +
                        "<p>此驗證碼有效時間為 5 分鐘，請勿洩漏給他人。</p>",
                otpCode);

        sendEmailViaMailjet(toEmail, subject, htmlContent);
    }

    private void sendEmailViaMailjet(String toEmail, String subject, String htmlContent) {
        // 檢查是否為預設的 Placeholder 或是未填入值
        if (isPlaceholderOrEmpty(apiKey) || isPlaceholderOrEmpty(secretKey) || isPlaceholderOrEmpty(senderEmail)) {
            log.warn("[Mailjet] 偵測到 Mailjet API Key/Secret 尚未設定（仍為預設值），跳過發信步驟。");
            return;
        }

        try {
            MailjetRequest payload = new MailjetRequest(List.of(
                    new MailjetMessage(
                            new MailjetContact(senderEmail, senderName),
                            List.of(new MailjetContact(toEmail, "會員")),
                            subject,
                            subject,
                            htmlContent)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Basic Auth Header
            String auth = apiKey + ":" + secretKey;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            HttpEntity<MailjetRequest> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(MAILJET_URL, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Mailjet] 電子郵件成功發送至 {}", toEmail);
            } else {
                log.error("[Mailjet] 發送郵件失敗，狀態碼：{}，回應內容：{}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("[Mailjet] 呼叫 Mailjet API 時發生異常，錯誤訊息：{}", e.getMessage());
        }
    }

    private boolean isPlaceholderOrEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.contains("YOUR_MAILJET");
    }

    // Mailjet API DTOs (使用 Java Records 簡潔表示)
    private record MailjetRequest(List<MailjetMessage> Messages) {
    }

    private record MailjetMessage(
            MailjetContact From,
            List<MailjetContact> To,
            String Subject,
            String TextPart,
            String HTMLPart) {
    }

    private record MailjetContact(String Email, String Name) {
    }
}
