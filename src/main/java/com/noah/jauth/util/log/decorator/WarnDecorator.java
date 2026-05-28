package com.noah.jauth.util.log.decorator;

public class WarnDecorator implements Decorator {

    private static final String ANSI_YELLOW = "\033[33m";
    
    @Override
    public String decorate(String message) {
        String outMessage = "WARN: " + message;
        if (isAnsiTerminal()) {
            return ANSI_YELLOW + outMessage + ANSI_RESET;
        }
        return outMessage;
    }
}
