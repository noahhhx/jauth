package com.noah.jauth;

import com.noah.jauth.commands.CommandRegistry;
import com.noah.jauth.commands.Connect;
import com.noah.jauth.commands.Help;
import com.noah.jauth.util.InputParser;
import com.noah.jauth.util.log.Level;
import com.noah.jauth.util.log.Logger;

public class Main {
    
    private static final CommandRegistry registry = new CommandRegistry();

    public static void main(String[] args) {
        registerCommands();
        
        if (args.length == 0) {
            registry.execute("help");
            return;
        }
        
        setLogLevel(args);
        
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
    
    private static void setLogLevel(String[] args) {
        for (String arg: args) {
            if (arg.contains("--log-level")) {
                String level = arg.split("=")[1];
                Logger.setLevel(Level.fromString(level));
            }
        }
        //Logger.setLevel();
    }
}
