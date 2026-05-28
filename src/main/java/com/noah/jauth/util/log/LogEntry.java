package com.noah.jauth.util.log;

public class LogEntry {

    private final Level level;
    private final String message;
    
    public LogEntry(Level level, String message) {
        this.level = level;
        this.message = message;
    }
    
    public Level getLevel() {
        return this.level;
    }
    
    public String getMessage() {
        return this.message;
    }
}
