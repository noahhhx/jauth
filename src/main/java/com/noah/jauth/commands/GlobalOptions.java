package com.noah.jauth.commands;

import com.noah.jauth.util.log.Level;
import com.noah.jauth.util.log.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalOptions {

    private static final Option<Level> logLevel = new Option<>(
          List.of("--log-level"), "Set the logging level for the program run.",
          Level.INFO, Level::fromString
    );

    public static Level getLogLevel() {
        return logLevel.getValue();
    }

    public static List<Option<?>> getOptions() {
        return List.of(logLevel);
    }

    public static String[] parse(String[] args) {
        Map<String, Option<?>> options = new HashMap<>();
        List<String> retVal = Arrays.asList(args);
        getOptions().forEach(opt -> {
            opt.getNames().forEach(name -> {
                options.put(name, opt);
                  }
            );
        });

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (arg.contains("=")) {
                    String[] parts = arg.split("=", 2);
                    if (!options.containsKey(parts[0])) {
                        continue;
                    }
                    Option<?> option = options.get(parts[0]);
                    option.setValue(parts[1]);
                    // Remove arg from array so we don't have to parse again
                    retVal = removeAt(args, i);
                }
            } else if (arg.contains("-")) {
                // TODO
                Logger.error("Yeah you need to do your todos bro");
            }
        }
        return retVal.toArray(new String[0]);
    }

    private static List<String> removeAt(String[] arr, int index) {
        String[] result = new String[arr.length - 1];
        System.arraycopy(arr, 0, result, 0, index);
        System.arraycopy(arr, index + 1, result, index, arr.length - index - 1);
        return Arrays.asList(result);
    }
}
