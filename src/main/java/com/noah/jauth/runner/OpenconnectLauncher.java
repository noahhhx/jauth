package com.noah.jauth.runner;

import com.noah.jauth.auth.AuthCompleteResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.tinylog.Logger;

/**
 * Spawns the openconnect binary and pipes the SAML session token into its
 * stdin.
 */
public class OpenconnectLauncher {

    public static final class Config {
        public String host;
        public String acVersion;
        public String openconnectPath = "openconnect";
        public boolean useSudo = true;
        public String sudoBinary;       // overrides auto-detect when non-null
        public List<String> extraArgs = List.of();
    }

    private OpenconnectLauncher() {}

    public static int launch(Config cfg, AuthCompleteResponse complete) throws Exception {
        List<String> cmd = buildCommand(cfg, complete);
        Logger.info("Launching: {}", String.join(" ", cmd));

        ProcessBuilder pb =
              new ProcessBuilder(cmd)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT);
        // stdin defaults to PIPE so we can write the session token below.

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            String hint =
                  cfg.useSudo
                        ? "Make sure sudo and openconnect are installed and on PATH."
                        : "Make sure openconnect is installed and on PATH.";
            throw new IOException("Cannot launch openconnect: " + e.getMessage() + " " + hint, e);
        }

        try (OutputStream stdin = p.getOutputStream()) {
            stdin.write(complete.sessionToken().getBytes(StandardCharsets.UTF_8));
        }

        return p.waitFor();
    }

    static List<String> buildCommand(Config cfg, AuthCompleteResponse complete) {
        List<String> cmd = new ArrayList<>();
        if (cfg.useSudo && !isWindows() && !isRoot()) {
            cmd.add(cfg.sudoBinary != null ? cfg.sudoBinary : pickPrivilegedRunner());
        }
        cmd.add(cfg.openconnectPath);
        cmd.add("--useragent");
        cmd.add("AnyConnect Linux_64 " + cfg.acVersion);
        cmd.add("--version-string");
        cmd.add(cfg.acVersion);
        cmd.add("--cookie-on-stdin");
        cmd.add("--servercert");
        cmd.add(complete.serverCertHash());
        cmd.addAll(cfg.extraArgs);
        cmd.add(cfg.host);
        return cmd;
    }

    /** Prefer {@code doas} when present (BSD-friendly), fall back to {@code sudo}. */
    private static String pickPrivilegedRunner() {
        for (String name : new String[] {"doas", "sudo"}) {
            if (which(name) != null) return name;
        }
        // sudo not found, but the user might still have it under another
        // PATH at exec time — fall through and let ProcessBuilder fail loudly.
        return "sudo";
    }

    private static String which(String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(java.io.File.pathSeparator)) {
            java.io.File f = new java.io.File(dir, name);
            if (f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * Best-effort check whether we're already root. Avoids prompting for sudo
     * when the user already ran us with elevated privileges.
     */
    private static boolean isRoot() {
        if (isWindows()) return false;
        // /proc/self/status reports the effective UID portably on Linux; macOS
        // doesn't have /proc, so fall back to user.name=="root".
        try {
            java.nio.file.Path status = java.nio.file.Paths.get("/proc/self/status");
            if (java.nio.file.Files.exists(status)) {
                for (String line : java.nio.file.Files.readAllLines(status)) {
                    if (line.startsWith("Uid:")) {
                        String[] parts = line.split("\\s+");
                        // Uid: ruid euid suid fsuid — effective uid is index 2.
                        return parts.length >= 3 && "0".equals(parts[2]);
                    }
                }
            }
        } catch (IOException ignored) {
            // Fall through to the user.name check.
        }
        return "root".equals(System.getProperty("user.name"));
    }
}

