package com.noah.jauth.commands;

import java.util.List;

public abstract class Command {

    public abstract int execute();
    public abstract String getHelp();
    public abstract String getName();
    public abstract List<Option<?>> getOptions();
}
