package com.noah.jauth.util.log.decorator;

public class ErrorDecorator implements Decorator {

    public static final String ANSI_RED = "\033[31m";
    
    @Override
    public String decorate(String message) {
        String outMessage = "ERROR: " + message;
        if (isAnsiTerminal()) {
            return ANSI_RED + outMessage + ANSI_RESET;
        }
        return outMessage;
    }
}
