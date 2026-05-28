package com.noah.jauth.util.log;

import com.noah.jauth.util.log.decorator.DebugDecorator;
import com.noah.jauth.util.log.decorator.Decorator;
import com.noah.jauth.util.log.decorator.ErrorDecorator;
import com.noah.jauth.util.log.decorator.InfoDecorator;
import com.noah.jauth.util.log.decorator.WarnDecorator;

public class ConsoleWriter implements Writer {

    @Override
    public void write(LogEntry logEntry) {
        switch (logEntry.getLevel()) {
            case WARN -> {
                systemErr(new WarnDecorator(), logEntry.getMessage());
            }
            case DEBUG -> {
                systemOut(new DebugDecorator(), logEntry.getMessage());
            }
            case ERROR -> {
                systemErr(new ErrorDecorator(), logEntry.getMessage());
            }
            default -> {
                systemOut(new InfoDecorator(), logEntry.getMessage());
            }
        }
    }
    
    private void systemOut(Decorator decorator, String message) {
        System.out.println(decorator.decorate(message));
    }
    
    private void systemErr(Decorator decorator, String message) {
        System.err.println(decorator.decorate(message));
    }
}
