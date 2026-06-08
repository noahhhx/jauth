package com.noah.jauth.util.log.decorator;

public class ErrorDecorator implements Decorator {

    public static final String ANSI_RED = "\033[31m";
    private static final String ERROR = "ERROR: ";
    
    @Override
    public String decorate(String message) {
        String startMessage = isAnsiTerminal()
              ? ANSI_RED + ERROR + ANSI_RESET : ERROR;
        return startMessage + message;
    }
}
