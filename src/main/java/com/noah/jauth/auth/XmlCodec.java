package com.noah.jauth.auth;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Reads and writes the Cisco AnyConnect aggregate-auth-v2 XML messages.
 * Parser is configured to reject external entities (XXE-safe).
 */
public class XmlCodec {

    private static final XMLInputFactory IN = newSafeInputFactory();
    private static final XMLOutputFactory OUT = XMLOutputFactory.newInstance();

    private XmlCodec() {}

    private static XMLInputFactory newSafeInputFactory() {
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        return f;
    }

    public static String encodeAuthInit(String host, String groupAccessUrl, String acVersion) {
        StringWriter sw = new StringWriter();
        try {
            XMLStreamWriter w = OUT.createXMLStreamWriter(sw);
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("config-auth");
            w.writeAttribute("client", "vpn");
            w.writeAttribute("type", "init");
            w.writeAttribute("aggregate-auth-version", "2");

            w.writeStartElement("version");
            w.writeAttribute("who", "vpn");
            w.writeCharacters(acVersion);
            w.writeEndElement();

            simpleElement(w, "device-id", "linux-64");
            simpleElement(w, "group-select", host);
            simpleElement(w, "group-access", groupAccessUrl);

            w.writeStartElement("capabilities");
            simpleElement(w, "auth-method", "single-sign-on-v2");
            w.writeEndElement();

            w.writeEndElement();
            w.writeEndDocument();
            w.close();
        } catch (XMLStreamException e) {
            throw new AuthProtocolException("Failed to encode auth-init request", e);
        }
        return sw.toString();
    }

    public static String encodeAuthReply(
          String acVersion, String hostname, String opaqueXml, String ssoToken) {
        // Built by string concatenation rather than StAX because the opaque
        // block must be spliced in verbatim and StAX defers empty-element
        // closes ("/>") until the next write, which corrupts the document
        // when we bypass the writer for the splice.
        StringBuilder b = new StringBuilder(512);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<config-auth client=\"vpn\" type=\"auth-reply\" aggregate-auth-version=\"2\">\n");
        b.append("  <version who=\"vpn\">").append(escape(acVersion)).append("</version>\n");
        b.append("  <device-id computer-name=\"").append(escape(hostname)).append("\">linux-64</device-id>\n");
        b.append("  <session-token/>\n");
        b.append("  <session-id/>\n");
        b.append("  ").append(opaqueXml).append('\n');
        b.append("  <auth><sso-token>").append(escape(ssoToken)).append("</sso-token></auth>\n");
        b.append("</config-auth>\n");
        return b.toString();
    }

    /** Minimal XML attribute/text escaping. */
    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static AuthRequestResponse parseAuthRequest(byte[] xml) {
        // Extract <opaque>...</opaque> by byte range, not via StAX re-
        // serialization. The gateway expects us to echo opaque verbatim in
        // the auth-reply; StAX may reorder attributes, change whitespace, or
        // strip namespace prefixes, and Cisco will then reject the request
        // with "VPN Server could not parse request".
        String opaqueXml = extractOpaqueVerbatim(xml);

        XMLStreamReader r = newReader(xml);
        try {
            String loginUrl = null;
            String loginFinalUrl = null;
            String tokenCookieName = null;
            String authError = null;
            String responseType = null;

            while (r.hasNext()) {
                int event = r.next();
                if (event != XMLStreamConstants.START_ELEMENT) continue;
                String name = r.getLocalName();
                switch (name) {
                    case "config-auth" -> responseType = r.getAttributeValue(null, "type");
                    case "sso-v2-login" -> loginUrl = r.getElementText();
                    case "sso-v2-login-final" -> loginFinalUrl = r.getElementText();
                    case "sso-v2-token-cookie-name" -> tokenCookieName = r.getElementText();
                    case "error" -> authError = r.getElementText();
                    case "opaque" -> skipElement(r);
                    default -> {}
                }
            }

            if (responseType != null && !responseType.equals("auth-request")) {
                throw new AuthProtocolException(
                      "Expected config-auth type=auth-request, got type=" + responseType
                            + (authError != null ? " (" + authError + ")" : ""));
            }
            if (authError != null) {
                throw new AuthProtocolException("Gateway returned error: " + authError);
            }
            if (loginUrl == null) {
                throw new AuthProtocolException(
                      "auth-request is missing sso-v2-login — does this gateway use SSO v2?");
            }
            if (opaqueXml == null) {
                throw new AuthProtocolException("auth-request is missing opaque block");
            }
            return new AuthRequestResponse(
                  loginUrl,
                  loginFinalUrl != null ? loginFinalUrl : loginUrl,
                  tokenCookieName != null ? tokenCookieName : "webvpn",
                  opaqueXml);
        } catch (XMLStreamException e) {
            throw new AuthProtocolException("Malformed auth-request XML", e);
        } finally {
            close(r);
        }
    }

    public static AuthCompleteResponse parseAuthComplete(byte[] xml) {
        XMLStreamReader r = newReader(xml);
        try {
            String responseType = null;
            String sessionToken = null;
            String serverCertHash = null;
            String authError = null;
            int depth = 0;
            // Track lineage so we only pick the right server-cert-hash if
            // there happen to be multiple <server-cert-hash> elements (one
            // under config/vpn-base-config/, one stray). The Cisco protocol
            // only has the one but defensive coding doesn't hurt.
            while (r.hasNext()) {
                int event = r.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String name = r.getLocalName();
                    switch (name) {
                        case "config-auth" -> responseType = r.getAttributeValue(null, "type");
                        case "session-token" -> sessionToken = r.getElementText();
                        case "server-cert-hash" -> {
                            if (serverCertHash == null) {
                                serverCertHash = r.getElementText();
                            }
                        }
                        case "error" -> authError = r.getElementText();
                        default -> {}
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                }
            }

            if (responseType != null && !responseType.equals("complete")) {
                throw new AuthProtocolException(
                      "Expected config-auth type=complete, got type=" + responseType
                            + (authError != null ? " (" + authError + ")" : ""));
            }
            if (authError != null) {
                throw new AuthProtocolException("Gateway returned error: " + authError);
            }
            if (sessionToken == null || sessionToken.isBlank()) {
                throw new AuthProtocolException("complete response missing session-token");
            }
            if (serverCertHash == null || serverCertHash.isBlank()) {
                throw new AuthProtocolException("complete response missing server-cert-hash");
            }
            return new AuthCompleteResponse(sessionToken, serverCertHash);
        } catch (XMLStreamException e) {
            throw new AuthProtocolException("Malformed complete XML", e);
        } finally {
            close(r);
        }
    }

    private static XMLStreamReader newReader(byte[] xml) {
        try {
            return IN.createXMLStreamReader(new ByteArrayInputStream(xml), "UTF-8");
        } catch (XMLStreamException e) {
            throw new AuthProtocolException("Cannot create XML reader", e);
        }
    }

    private static void simpleElement(XMLStreamWriter w, String name, String text)
          throws XMLStreamException {
        w.writeStartElement(name);
        if (text != null) w.writeCharacters(text);
        w.writeEndElement();
    }

    /** Advances the reader past the END_ELEMENT of the current element. */
    private static void skipElement(XMLStreamReader r) throws XMLStreamException {
        int depth = 1;
        while (r.hasNext() && depth > 0) {
            int e = r.next();
            if (e == XMLStreamConstants.START_ELEMENT) depth++;
            else if (e == XMLStreamConstants.END_ELEMENT) depth--;
        }
    }

    /**
     * Extracts the {@code <opaque>...</opaque>} element from the response as
     * a verbatim byte-range substring. Cisco echoes auth state through this
     * blob and rejects auth-reply if anything inside it changes — even
     * whitespace or attribute order.
     */
    static String extractOpaqueVerbatim(byte[] xml) {
        String s = new String(xml, StandardCharsets.UTF_8);
        int start = indexOfOpaqueOpen(s);
        if (start < 0) return null;
        int openTagEnd = s.indexOf('>', start);
        if (openTagEnd < 0) return null;
        // Self-closing <opaque/> or <opaque .../>
        if (s.charAt(openTagEnd - 1) == '/') {
            return s.substring(start, openTagEnd + 1);
        }
        int closeTag = s.indexOf("</opaque>", openTagEnd);
        if (closeTag < 0) return null;
        return s.substring(start, closeTag + "</opaque>".length());
    }

    /**
     * Finds the start of the opening {@code <opaque} tag (with either a
     * trailing space, slash, or {@code >}). Avoids matching e.g.
     * {@code <opaque-foo>} or random text inside another element.
     */
    private static int indexOfOpaqueOpen(String s) {
        int from = 0;
        while (true) {
            int hit = s.indexOf("<opaque", from);
            if (hit < 0) return -1;
            char next = s.charAt(hit + "<opaque".length());
            if (next == ' ' || next == '\t' || next == '\n' || next == '\r'
                  || next == '/' || next == '>') {
                return hit;
            }
            from = hit + 1;
        }
    }

    private static void close(XMLStreamReader r) {
        try {
            r.close();
        } catch (XMLStreamException ignored) {
            // closing a reader on a ByteArrayInputStream cannot meaningfully fail
        }
    }

    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
