package com.noah.jauth.util.log.decorator;

public interface Decorator {

    String ANSI_RESET = "\033[0m";
    
    String decorate(String message);
    
    default boolean isAnsiTerminal() {
        return System.console() != null;
    }
}
