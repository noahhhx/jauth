package com.noah.jauth.browser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.noah.jauth.util.log.Logger;

/**
 * Launches an external browser pointed at a URL. Either use OS default or explicit binary path.
 */
public class SystemBrowser {

    private final String explicitPath;
    private final List<String> extraArgs;

    public SystemBrowser(String explicitPath, List<String> extraArgs) {
        this.explicitPath = explicitPath;
        this.extraArgs = extraArgs != null ? List.copyOf(extraArgs) : List.of();
    }

    public void open(String url) throws IOException {
        List<String> cmd = new ArrayList<>();
        if (explicitPath != null && !explicitPath.isBlank()) {
            cmd.add(explicitPath);
            cmd.addAll(extraArgs);
            cmd.add(url);
        } else {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac") || os.contains("darwin")) {
                Logger.error("Get off mac...");
                System.exit(1);
            } else if (os.contains("win")) {
                Logger.error("Get off windows....");
                System.exit(1);
            } else {
                cmd.add("xdg-open");
                cmd.addAll(extraArgs);
                cmd.add(url);
            }
        }
        new ProcessBuilder(cmd).inheritIO().start();
    }
}
