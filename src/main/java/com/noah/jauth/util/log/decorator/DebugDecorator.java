package com.noah.jauth.util.log.decorator;

public class DebugDecorator implements Decorator {

    public static final String ANSI_CYAN = "\033[36m";
    private static final String DEBUG = "DEBUG: ";
    
    @Override
    public String decorate(String message) {
        String startMessage = isAnsiTerminal()
              ? ANSI_CYAN + DEBUG + ANSI_RESET : DEBUG;
        return startMessage + message;
    }
}
