package com.axuanhogan.membership.service;

import org.springframework.stereotype.Service;

import com.axuanhogan.membership.dao.User;
import com.axuanhogan.membership.repository.UserRepository;

import java.time.ZonedDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 更新最後登入時間
     */
    public void updateLastSignInAt(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("使用者資料異常"));

        // 更新最後登入時間為目前時間
        user.setLastSignInAt(ZonedDateTime.now());
        userRepository.save(user);
    }

    /**
     * 根據 Email 查詢使用者
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("帳號不存在"));
    }
}
