package com.noah.jauth.http;

public class GatewayHttpException extends RuntimeException {

    public GatewayHttpException(String message) {
        super(message);
    }
    
    public GatewayHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
