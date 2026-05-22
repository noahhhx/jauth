package com.noah.jauth.commands;

import java.util.List;
import java.util.function.Function;

public class Option<T> {

    private final List<String> names;
    private final String description;
    private final T defaultValue;
    private T value;
    private final Function<String, T> parser;

    public Option(List<String> names, String description, T defaultValue,
          Function<String, T> parser) {
        this.names = names;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.parser = parser;
    }

    public List<String> getNames() {
        return names;
    }

    public String getDescription() {
        return description;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(String raw) {
        this.value = parser.apply(raw);
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }
}
