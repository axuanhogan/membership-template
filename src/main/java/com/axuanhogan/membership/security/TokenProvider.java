package com.axuanhogan.membership.security;

public interface TokenProvider {
    String generatePreAuthToken(String email);
    String generateAccessToken(String email);
    String getEmailFromToken(String token);
    String getTokenTypeFromToken(String token);
    boolean validateToken(String token);
}
