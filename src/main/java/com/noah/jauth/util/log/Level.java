package com.noah.jauth.util.log;

public enum Level {
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public static Level fromString(String level) {
        try {
            return Level.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warn(
                  "Wrong argument `{}` set for `--log-level`. Valid options are [ {}, {}, {}, {} ]",
                  level, DEBUG, INFO, WARN, ERROR);
            return Level.INFO;
        }
    }
}
