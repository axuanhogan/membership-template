package com.axuanhogan.membership;

import com.axuanhogan.membership.dao.OtpToken;
import com.axuanhogan.membership.dao.User;
import com.axuanhogan.membership.repository.OtpTokenRepository;
import com.axuanhogan.membership.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MembershipApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        otpTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("null")
    public void testFullUserLifecycle() throws Exception {
        String email = "test@example.com";
        String password = "securePassword123";

        // 1. 會員註冊 (POST /v1/auth/sign-up)
        String registerJson = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        mockMvc.perform(post("/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("註冊成功")));

        User user = userRepository.findByEmail(email).orElse(null);
        assertNotNull(user);
        assertFalse(user.isEnabled());
        assertNotNull(user.getActivationToken());

        // 嘗試在未開通前進行登入 (預期失敗 400 Bad Request)
        String signInJson = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        mockMvc.perform(post("/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signInJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("尚未開通")));

        // 2. 帳號啟用 (GET /v1/auth/sign-up/activate)
        mockMvc.perform(get("/v1/auth/sign-up/activate")
                        .param("token", user.getActivationToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("帳號開通成功")));

        user = userRepository.findByEmail(email).orElse(null);
        assertNotNull(user);
        assertTrue(user.isEnabled());
        assertNull(user.getActivationToken());

        // 3. 會員登入第一階段 - 密碼驗證 (POST /v1/auth/sign-in)
        MvcResult signInResult = mockMvc.perform(post("/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signInJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.preAuthToken", notNullValue()))
                .andReturn();

        String responseContent = signInResult.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseContent, Map.class);
        Map<?, ?> dataMap = (Map<?, ?>) responseMap.get("data");
        String preAuthToken = (String) dataMap.get("preAuthToken");

        // 從資料庫取得系統產生的 OTP
        OtpToken otpToken = otpTokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals(email))
                .findFirst().orElse(null);
        assertNotNull(otpToken);
        assertFalse(otpToken.isUsed());
        String otpCode = otpToken.getOtpCode();

        // 4. 會員登入第二階段 - 2FA 驗證 (PATCH /v1/auth/sign-in)
        String verifyJson = objectMapper.writeValueAsString(Map.of(
                "preAuthToken", preAuthToken,
                "otpCode", otpCode
        ));

        MvcResult verifyResult = mockMvc.perform(patch("/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andReturn();

        String verifyResponseContent = verifyResult.getResponse().getContentAsString();
        Map<?, ?> verifyResponseMap = objectMapper.readValue(verifyResponseContent, Map.class);
        Map<?, ?> verifyDataMap = (Map<?, ?>) verifyResponseMap.get("data");
        String accessToken = (String) verifyDataMap.get("accessToken");

        // 驗證 OTP 是否已被標記為已使用
        otpToken = otpTokenRepository.findById(otpToken.getId()).orElse(null);
        assertNotNull(otpToken);
        assertTrue(otpToken.isUsed());

        // 5. 查詢最後登入時間 - 未授權 (預期 403 Forbidden)
        mockMvc.perform(get("/v1/users/me/last-sign-in"))
                .andExpect(status().isForbidden());

        // 6. 查詢最後登入時間 - 已授權 (GET /v1/users/me/last-sign-in，帶 Bearer Token)
        mockMvc.perform(get("/v1/users/me/last-sign-in")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.lastSignInAt", anyOf(
                        containsString("+08:00"),
                        containsString("UTC+8"),
                        containsString("GMT+8")
                )));
    }
}
