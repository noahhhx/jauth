package com.noah.jauth.commands;

import java.util.List;

public class Help extends Command {
    private final CommandRegistry registry;
    public Help(CommandRegistry registry) {
        this.registry = registry;
    }
    @Override
    public int execute() {
        registry.getAllCommands().forEach((name, command) -> {
            if ("help".equals(name)) return;
            System.out.printf("Usage: jauth %s [OPTIONS]%n", command.getName());
            System.out.printf("  %s", command.getHelp());

            List<Option<?>> options = command.getOptions();
            if (!options.isEmpty()) {
                System.out.println("Options:");
                for (Option<?> opt : options) {
                    String def = opt.getDefaultValue() != null
                          ? " (default: " + opt.getDefaultValue() + ")"
                          : "";
                    System.out.printf("    %-30s %s%s%n",
                          String.join(", ", opt.getNames()),
                          opt.getDescription(), def);
                }
            }
        });
        return 0;
    }
    @Override
    public String getHelp() {
        return "";
    }
    @Override
    public String getName() {
        return "help";
    }
    @Override
    public List<Option<?>> getOptions() {
        return List.of();
    }
}