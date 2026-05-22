package com.noah.jauth.auth;

public class AuthProtocolException extends RuntimeException {
    
    public AuthProtocolException(String message) {
        super(message);
    }

    public AuthProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
