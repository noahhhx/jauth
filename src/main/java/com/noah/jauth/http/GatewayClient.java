package com.noah.jauth.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Thin wrapper over {@link HttpClient} that:
 *  - sets AnyConnect headers
 *  - optionally disables TLS verification
 */
public class GatewayClient {
    
    private final HttpClient probeClient;
    private final HttpClient authClient;
    private final String acVersion;
    private final Duration timeout;
    
    public GatewayClient(String acVersion, Duration timeout, boolean verifyTls) {
        this.acVersion = acVersion;
        this.timeout = timeout;
        HttpClient.Builder probeBuilder =
              HttpClient.newBuilder()
                    .followRedirects(Redirect.NORMAL)
                    .connectTimeout(timeout);
        HttpClient.Builder authBuilder =
              HttpClient.newBuilder()
                    .followRedirects(Redirect.NEVER)
                    .connectTimeout(timeout);
        if (!verifyTls) {
            SSLContext insecure = insecureContext();
            probeBuilder.sslContext(insecure);
            authBuilder.sslContext(insecure);
        }
        this.probeClient = probeBuilder.build();
        this.authClient = authBuilder.build();
    }
    
    /** Follow redirects with plain GET req, return final URL **/
    public URI probe(URI uri) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(uri).GET().timeout(timeout).build();
        HttpResponse<Void> resp = probeClient.send(req, BodyHandlers.discarding());
        if (resp.statusCode() >= 400) {
            throw new GatewayHttpException(
                  "Gateway probe failed: HTTP " + resp.statusCode() + " for " + uri 
            );
        }
        return resp.uri();
    }
    
    /** POST the aggregate-auth XML payload to {@code endpoint} **/
    public byte[] postAuth(URI endpoint, String xml) throws Exception {
        HttpRequest req =
              HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("User-Agent", "AnyConnect Linux_64 " + acVersion)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("X-Transcend-Version", "1")
                    .header("X-Aggregate-Auth", "1")
                    .header("X-Support-HTTP-Auth", "true")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .build();
        HttpResponse<byte[]> resp = authClient.send(req, BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) {
            throw new GatewayHttpException(
                  "Gateway auth POST failed: HTTP " + resp.statusCode() + " for " + endpoint);
        }
        return resp.body();
    }
    
    
    private static SSLContext insecureContext() {
        TrustManager[] trustAll = {
              new X509TrustManager() {
                  @Override
                  public void checkClientTrusted(X509Certificate[] chain, String authType) {
                  }
                  @Override
                  public void checkServerTrusted(X509Certificate[] chain, String authType) {
                  }
                  @Override
                  public X509Certificate[] getAcceptedIssuers() {
                      return new X509Certificate[0];
                  }
              }
        };
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            return ctx;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot build insecure SSLContext", e);
        }
    }

}
