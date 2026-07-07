package com.axuanhogan.membership.dao;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "activation_token")
    private String activationToken;

    @Column(name = "activation_token_expires_at")
    private ZonedDateTime activationTokenExpiresAt;

    @Column(name = "last_sign_in_at")
    private ZonedDateTime lastSignInAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.enabled = false;
        this.createdAt = ZonedDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public ZonedDateTime getActivationTokenExpiresAt() {
        return activationTokenExpiresAt;
    }

    public void setActivationTokenExpiresAt(ZonedDateTime activationTokenExpiresAt) {
        this.activationTokenExpiresAt = activationTokenExpiresAt;
    }

    public ZonedDateTime getLastSignInAt() {
        return lastSignInAt;
    }

    public void setLastSignInAt(ZonedDateTime lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
