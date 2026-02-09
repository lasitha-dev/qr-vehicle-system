package com.uop.qrvehicle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak OIDC Configuration
 * Maps to app.oidc.* properties in application.properties
 * Mirrors the PHP OIDCConfig.php / EnvProd.php settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.oidc")
public class KeycloakOidcConfig {

    private String clientId = "vehicle-pass-app";
    private String clientSecret;
    private String issuerUri;
    private String redirectUri;
    private String postLogoutRedirectUri;
    private String sessionName = "KCSESSID";
    private int tokenRefreshThresholdSeconds = 60;
    private int tokenRefreshGraceSeconds = 30;

    // Getters and setters

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public int getTokenRefreshThresholdSeconds() {
        return tokenRefreshThresholdSeconds;
    }

    public void setTokenRefreshThresholdSeconds(int tokenRefreshThresholdSeconds) {
        this.tokenRefreshThresholdSeconds = tokenRefreshThresholdSeconds;
    }

    public int getTokenRefreshGraceSeconds() {
        return tokenRefreshGraceSeconds;
    }

    public void setTokenRefreshGraceSeconds(int tokenRefreshGraceSeconds) {
        this.tokenRefreshGraceSeconds = tokenRefreshGraceSeconds;
    }

    /**
     * Get the full end-session (logout) endpoint URL for Keycloak
     */
    public String getEndSessionEndpoint() {
        String issuer = issuerUri != null ? issuerUri.replaceAll("/+$", "") : "";
        return issuer + "/protocol/openid-connect/logout";
    }
}
