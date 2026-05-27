package com.noah.jauth.util;

import com.noah.jauth.commands.Command;
import com.noah.jauth.commands.Option;
import com.noah.jauth.commands.RequiredOption;
import com.noah.jauth.util.log.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputParser {

    /**
     * Parses CLI args and populates the command's options in place.
     */
    public static void parse(String[] args, Command command) {
        Map<String, Option<?>> flagIndex = new HashMap<>();
        List<String> requiredOpts = new ArrayList<>();
        for (Option<?> opt : command.getOptions()) {
            for (String name : opt.getNames()) {
                flagIndex.put(name, opt);
                if (opt instanceof RequiredOption<?>) {
                    requiredOpts.add(name);
                }
            }
        }
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (arg.contains("=")) {
                    String[] parts = arg.split("=", 2);
                    applyValue(flagIndex, parts[0], parts[1]);
                } else {
                    i = applyFlag(flagIndex, args, i, arg);
                }
            } else if (arg.startsWith("-") && arg.length() == 2) {
                i = applyFlag(flagIndex, args, i, arg);
            }
        }
        requiredOpts.forEach(opt -> {
            Option<?> requiredOpt = flagIndex.get(opt);
            if (requiredOpt.getValue() == null) {
                Logger.error(requiredOpt.getNames() + " is a required option.");
                System.exit(1);
            }
        });
    }

    private static int applyFlag(Map<String, Option<?>> index, String[] args, int i, String name) {
        Option<?> opt = index.get(name);
        if (opt == null) {
            return i;
        }
        if (opt.isBoolean()) {
            opt.setValue(null);
        } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
            opt.setValue(args[++i]);
        }
        return i;
    }

    private static void applyValue(Map<String, Option<?>> index,
          String name, String value) {
        Option<?> opt = index.get(name);
        if (opt != null) {
            opt.setValue(value);
        }
    }
}
