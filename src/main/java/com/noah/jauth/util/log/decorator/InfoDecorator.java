package com.noah.jauth.util.log.decorator;

public class InfoDecorator implements Decorator {

    public static final String ANSI_PURPLE = "\033[35m";
    private static final String INFO = "INFO: ";

    @Override
    public String decorate(String message) {
        // TODO refactor this repeated pattern
        String startMessage = isAnsiTerminal()
              ? ANSI_PURPLE + INFO + ANSI_RESET : INFO;
        return startMessage + message;
    }
}
