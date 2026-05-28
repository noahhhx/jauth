package com.noah.jauth.util.log.decorator;

public class DebugDecorator implements Decorator {

    public static final String ANSI_CYAN = "\033[36m";
    
    @Override
    public String decorate(String message) {
        String outMessage = "DEBUG: " + message;
        if (isAnsiTerminal()) {
            return ANSI_CYAN + outMessage + ANSI_RESET;
        }
        return outMessage;
    }
}
