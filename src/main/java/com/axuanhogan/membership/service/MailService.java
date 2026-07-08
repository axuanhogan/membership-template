package com.axuanhogan.membership.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Value("${mailgun.api-key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    @Value("${mailgun.base-url}")
    private String baseUrl;

    @Value("${mailgun.sender-email}")
    private String senderEmail;

    @Value("${mailgun.sender-name}")
    private String senderName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 發送開通帳號電子郵件
     */
    public void sendActivationEmail(String toEmail, String activationLink) {
        log.info("[Test Helper] 帳號開通連結 (若 Mailgun 未設定或發送失敗，可直接複製此連結測試): {}", activationLink);

        String subject = "會員帳號啟用信";
        String htmlContent = String.format(
                "<h3>歡迎註冊！</h3>" +
                        "<p>請點擊下方連結以開通您的帳號：</p>" +
                        "<a href='%s' target='_blank'>%s</a>" +
                        "<p>此連結有效時間為 30 分鐘。</p>",
                activationLink, activationLink);

        sendEmailViaMailgun(toEmail, subject, htmlContent);
    }

    /**
     * 發送兩階段驗證 OTP 電子郵件
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("[Test Helper] 兩階段驗證 OTP (若 Mailgun 未設定或發送失敗，可使用此驗證碼): {}", otpCode);

        String subject = "登入兩階段驗證碼";
        String htmlContent = String.format(
                "<h3>您好，</h3>" +
                        "<p>您的登入驗證碼為：<strong style='font-size: 24px; color: #1e88e5;'>%s</strong></p>" +
                        "<p>此驗證碼有效時間為 5 分鐘，請勿洩漏給他人。</p>",
                otpCode);

        sendEmailViaMailgun(toEmail, subject, htmlContent);
    }

    private void sendEmailViaMailgun(String toEmail, String subject, String htmlContent) {
        // 檢查是否為預設的 Placeholder 或是未填入值
        if (isPlaceholderOrEmpty(apiKey) || isPlaceholderOrEmpty(domain) || isPlaceholderOrEmpty(senderEmail)) {
            log.warn("[Mailgun] 偵測到 Mailgun API Key/Domain 尚未設定（仍為預設值），跳過發信步驟。");
            return;
        }

        try {
            String cleanBaseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://api.mailgun.net";
            String url = String.format("%s/v3/%s/messages", cleanBaseUrl, domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Basic Auth Header
            String auth = "api:" + apiKey;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
            headers.set("Authorization", authHeader);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("from", String.format("%s <%s>", senderName, senderEmail));
            form.add("to", toEmail);
            form.add("subject", subject);
            form.add("html", htmlContent);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Mailgun] 電子郵件成功發送至 {}", toEmail);
            } else {
                log.error("[Mailgun] 發送郵件失敗，狀態碼：{}，回應內容：{}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("[Mailgun] 呼叫 Mailgun API 時發生異常，錯誤訊息：{}", e.getMessage());
        }
    }

    private boolean isPlaceholderOrEmpty(String value) {
        return value == null || value.trim().isEmpty() || value.contains("YOUR_MAILGUN");
    }
}
