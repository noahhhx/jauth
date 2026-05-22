package com.noah.jauth.auth;

/**
 * Parsed response from the gateway's first {@code config-auth/init} POST.
 * Holds the URLs the browser must visit, and the opaque block we have to
 * echo back in the auth-reply.
 */
public record AuthRequestResponse(
      String loginUrl,
      String loginFinalUrl,
      String tokenCookieName,
      String opaqueXml) {
}
