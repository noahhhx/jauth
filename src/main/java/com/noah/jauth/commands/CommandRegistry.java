package com.noah.jauth.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandRegistry {

    private final Map<String, Command> commands = new HashMap<>();
    
    public void register(Command command) {
        commands.put(command.getName(), command);
    }
    
    public Optional<Command> getCommand(String commandName) {
        return Optional.ofNullable(commands.get(commandName));
    }
    
    public Map<String, Command> getAllCommands() {
        return commands;
    }
    
    public void execute(String name) {
        Optional<Command> command = getCommand(name);
        if (command.isPresent()) {
            command.get().execute();
        } else {
            System.out.printf("Unknown command: %s%n", name);
        }
    }
}