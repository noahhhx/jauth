package com.noah.jauth.util.log;

import java.util.List;

public final class Logger {
    
    private static volatile Level threshold = Level.INFO;
    private static final List<Writer> writers = List.of(new ConsoleWriter());
    
    private Logger() {}
    
    public static void setLevel(Level level) {
        threshold = level;
    }
    
    public static void debug(String msg, Object... args) {
        log(Level.DEBUG, msg, args);
    }

    public static void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    public static void warn(String msg, Object... args) {
        log(Level.WARN, msg, args);
    }

    public static void error(String msg, Object... args) {
        log(Level.ERROR, msg, args);
    }
    
    private static void log(Level level, String msg, Object... args) {
        if (level.ordinal() < threshold.ordinal()) {
            return;
        }
        LogEntry entry = new LogEntry(level, msg);
        writers.forEach(writer -> {
            writer.write(entry);
        });
    }
    
    private static String format(String msg, Object... args) {
        if (args == null || args.length == 0) {
            return msg;
        }
        StringBuilder sb = new StringBuilder(msg.length() + 32);
        int ai = 0, i = 0;
        while (i < msg.length()) {
            if (ai < args.length && i + 1 < msg.length() 
                  && msg.charAt(i) == '{' && msg.charAt(i+1) == '}') {
                sb.append(args[ai++]);
                i += 2;
            } else {
                sb.append(msg.charAt(i++));
            }
        }
        return sb.toString();
    }

}
