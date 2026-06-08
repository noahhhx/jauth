package com.noah.jauth.util.log.decorator;

public class WarnDecorator implements Decorator {

    private static final String ANSI_YELLOW = "\033[33m";
    private static final String WARN = "WARN: ";
    
    @Override
    public String decorate(String message) {
        String startMessage = isAnsiTerminal() 
              ? ANSI_YELLOW + WARN + ANSI_RESET : WARN;
        return startMessage + message;
    }
}
