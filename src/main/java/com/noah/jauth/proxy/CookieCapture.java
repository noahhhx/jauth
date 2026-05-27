package com.noah.jauth.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.noah.jauth.util.log.Logger;

/**
 * Captures the SSO token cookie after the user finishes signing in.
 *
 * <p>v0: manual paste — print instructions to stderr, then read the cookie
 * value from stdin. Works against any browser, no CA-trust prompt.
 *
 * <p>TODO(proxy): replace with a local MITM HTTPS proxy that scrapes the
 * {@code Set-Cookie} header on the gateway domain so the user never sees
 * devtools.
 */
public final class CookieCapture {

    private CookieCapture() {}

    public static String prompt(String tokenCookieName, String gatewayHost) throws IOException {
        StringBuilder banner = new StringBuilder();
        banner.append("\n");
        banner.append("--------------------------------------------------------------\n");
        banner.append("  Sign in in the browser window that just opened.\n");
        banner.append("  Once you land on the post-login page, copy the cookie value:\n");
        banner.append("\n");
        banner.append("    1. Open devtools (F12) on the ").append(gatewayHost).append(" page.\n");
        banner.append("    2. Go to the Cookies section:").append("\n");
        banner.append("        - Chromium: Application tab → Cookies → https://").append(gatewayHost).append("\n");
        banner.append("        - Firefox: Storage tab → Cookies → https://").append(gatewayHost).append("\n");
        banner.append("    3. Find the row named '").append(tokenCookieName).append("'.\n");
        banner.append("    4. Copy its Value, paste it below, press Enter.\n");
        banner.append("--------------------------------------------------------------\n");
        banner.append(tokenCookieName).append("=");
        System.err.print(banner);
        System.err.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line = in.readLine();
        if (line == null) {
            throw new IOException("EOF on stdin before cookie value was provided");
        }
        String value = line.trim();
        if (value.isEmpty()) {
            throw new IOException("Empty cookie value");
        }
        Logger.debug("Captured cookie value ({} chars)", value.length());
        return value;
    }
}
