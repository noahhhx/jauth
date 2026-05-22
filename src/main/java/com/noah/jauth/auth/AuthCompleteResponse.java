package com.noah.jauth.auth;

/**
 * Parsed response from the gateway's second {@code config-auth/auth-reply}
 * POST. Carries everything needed to invoke {@code openconnect}.
 */
public record AuthCompleteResponse(String sessionToken, String serverCertHash) {
}