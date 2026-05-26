package com.noah.jauth.commands;

import java.util.List;
import java.util.function.Function;

public class RequiredOption<T> extends Option<T> {

    public RequiredOption(List<String> names, String description,
          Function<String, T> parser) {
        super(names, description, null, parser);
    }
    
    @Override
    public String getDescription() {
        return super.getDescription() + " [REQUIRED]";
    }
}
