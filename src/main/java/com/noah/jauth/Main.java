package com.noah.jauth;

import com.noah.jauth.commands.CommandRegistry;
import com.noah.jauth.commands.Connect;
import com.noah.jauth.commands.Help;
import com.noah.jauth.util.InputParser;

public class Main {
    
    private static final CommandRegistry registry = new CommandRegistry();

    public static void main(String[] args) {
        registerCommands();
        if (args.length == 0) {
            registry.execute("help");
            return;
        }
        String commandName = args[0];
        registry.getCommand(commandName).ifPresentOrElse(
              cmd -> {
                  InputParser.parse(args, cmd);
                  System.exit(cmd.execute());
              },
              () -> System.out.printf(
                    "Unknown command: %s%n  Type 'help' to see available commands.%n", 
                    commandName)
        );
    }
    private static void registerCommands() {
        registry.register(new Connect());
        registry.register(new Help(registry));
    }
}
