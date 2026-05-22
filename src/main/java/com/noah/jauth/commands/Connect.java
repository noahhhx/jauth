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
    
    private final Option<Integer> timeoutSeconds = new Option<>(
          List.of("--timeout"), "Per-request timeout in seconds",
          30, Integer::parseInt); 
    
    private final Option<String> acVersion = new Option<>(
          List.of("--ac-version"), "AnyConnect version string",
          "5.1.4.74", Function.identity());
    
    private final Option<Boolean> noCertCheck = new Option<>(
          List.of("--no-cert-check"), "Skip TLS verification of the gateway",
          false, Boolean::parseBoolean);

    private final Option<String> host = new Option<>(
          List.of("--host"), "VPN gateway hostname",
          "stub-vpnhost", Function.identity());
    
    private final Option<String> browserPath = new Option<>(
          List.of("--browser"), "Path to browser to use",
          null, Function.identity());
    
    // TODO custom option
    List<String> browserArgs;
    
    private final Option<String> openconnectPath = new Option<>(
          List.of("--openconnect"), "Path to openconnect binary.",
          "openconnect", Function.identity());
    
    private final Option<Boolean> noSudo = new Option<>(
          List.of("--no-sudo"), "Don't prepend sudo when launching openconnect.",
          false, Boolean::parseBoolean);

    // TODO custom option
    List<String> openconnectArgs;


    @Override
    public int execute() {
        Duration timeout = Duration.ofSeconds(timeoutSeconds.getValue());
        GatewayClient http = new GatewayClient(acVersion.getValue(), timeout, !noCertCheck.getValue());

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
                  acVersion.getValue());
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
            new SystemBrowser(browserPath.getValue(), browserArgs).open(authReq.loginUrl());
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
                  XmlCodec.encodeAuthReply(acVersion.getValue(), hostname(), authReq.opaqueXml(), ssoToken);
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
        cfg.acVersion = acVersion.getValue();
        cfg.openconnectPath = openconnectPath.getValue();
        cfg.useSudo = !noSudo.getValue();
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
        return List.of(
              host, timeoutSeconds, browserPath, acVersion, 
              noCertCheck, openconnectPath, noSudo
        );
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
