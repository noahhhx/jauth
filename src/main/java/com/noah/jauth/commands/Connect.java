package com.noah.jauth.commands;

import com.noah.jauth.auth.AuthCompleteResponse;
import com.noah.jauth.auth.AuthProtocolException;
import com.noah.jauth.auth.AuthRequestResponse;
import com.noah.jauth.auth.XmlCodec;
import com.noah.jauth.browser.SystemBrowser;
import com.noah.jauth.http.GatewayClient;
import com.noah.jauth.http.GatewayHttpException;
import com.noah.jauth.proxy.CookieCapture;
import com.noah.jauth.runner.OpenconnectLauncher;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.tinylog.Logger;

public class Connect extends Command {

    // TODO custom option
    int timeoutSeconds = 30;

    // TODO custom option
    String acVersion = "5.1.4.74";

    // TODO custom option
    boolean noCertCheck;

    private final Option<String> host = new Option<>(
          List.of("--host", "-h"), "VPN gateway hostname",
          "stub-vpnhost", Function.identity()
    );

    // TODO custom option
    String browserPath;

    // TODO custom option
    List<String> browserArgs;

    // TODO custom option
    String openconnectPath = "openconnect";

    // TODO custom option
    boolean noSudo;

    // TODO custom option
    List<String> openconnectArgs;


    @Override
    public int execute() {
        Duration timeout = Duration.ofSeconds(30);
        GatewayClient http = new GatewayClient(acVersion, timeout, !noCertCheck);

        URI gatewayUrl = URI.create("https://" + host.getValue() + "/");
        URI authEndpoint;
        try {
            Logger.info("Probing {}", gatewayUrl);
            authEndpoint = http.probe(gatewayUrl);
            Logger.debug("Auth endpoint resolved to {}", authEndpoint);
        } catch (SSLException e) {
            Logger.error(
                  "TLS verification failed against the gateway. If it's a self-signed "
                        + "corporate cert, rerun with --no-cert-check. ({})",
                  e.getMessage());
            return 4;
        } catch (Exception e) {
            Logger.error("Network error probing gateway: {}", e.getMessage());
            return 4;
        }

        AuthRequestResponse authReq;
        try {
            String initXml = XmlCodec.encodeAuthInit(host.getValue(), authEndpoint.toString(),
                  acVersion);
            Logger.debug("auth-init request XML:\n{}", initXml);
            byte[] respBytes = http.postAuth(authEndpoint, initXml);
            Logger.debug("auth-init response XML:\n{}", new String(respBytes));
            authReq = XmlCodec.parseAuthRequest(respBytes);
            Logger.info("Login URL: {}", authReq.loginUrl());
            Logger.debug("Opaque echo block:\n{}", authReq.opaqueXml());
        } catch (AuthProtocolException e) {
            Logger.error("Gateway protocol error: {}", e.getMessage());
            return 3;
        } catch (GatewayHttpException e) {
            Logger.error("{}", e.getMessage());
            return 4;
        } catch (Exception e) {
            Logger.error("Auth-init failed: {}", e.getMessage());
            return 4;
        }

        try {
            new SystemBrowser(browserPath, browserArgs).open(authReq.loginUrl());
        } catch (Exception e) {
            Logger.error("Could not launch browser: {}", e.getMessage());
            return 2;
        }

        String ssoToken;
        try {
            ssoToken = CookieCapture.prompt(authReq.tokenCookieName(), host.getValue());
        } catch (Exception e) {
            Logger.error("Cookie capture aborted: {}", e.getMessage());
            return 2;
        }

        AuthCompleteResponse complete;
        try {
            String replyXml =
                  XmlCodec.encodeAuthReply(acVersion, hostname(), authReq.opaqueXml(), ssoToken);
            Logger.debug("auth-reply request XML:\n{}", replyXml);
            byte[] respBytes = http.postAuth(authEndpoint, replyXml);
            Logger.debug("auth-reply response XML:\n{}", new String(respBytes));
            complete = XmlCodec.parseAuthComplete(respBytes);
        } catch (AuthProtocolException e) {
            Logger.error("Gateway rejected auth-reply: {}", e.getMessage());
            return 3;
        } catch (GatewayHttpException e) {
            Logger.error("{}", e.getMessage());
            return 4;
        } catch (Exception e) {
            Logger.error("Auth-reply failed: {}", e.getMessage());
            return 4;
        }

        OpenconnectLauncher.Config cfg = new OpenconnectLauncher.Config();
        cfg.host = host.getValue();
        cfg.acVersion = acVersion;
        cfg.openconnectPath = openconnectPath;
        cfg.useSudo = !noSudo;
        cfg.extraArgs = openconnectArgs != null ? openconnectArgs : List.of();
        try {
            System.exit(OpenconnectLauncher.launch(cfg, complete));
        } catch (Exception e) {
            Logger.error("openconnect launch failed: {}", e.getMessage());
            return 3;
        }
        return 0;
    }

    @Override
    public String getHelp() {
        return """
              Connect to openconnect VPN via AnyConnect SAML
              """;
    }

    @Override
    public String getName() {
        return "connect";
    }

    @Override
    public List<Option<?>> getOptions() {
        return List.of(host);
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
