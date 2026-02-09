package com.uop.qrvehicle.security;

import com.uop.qrvehicle.config.KeycloakOidcConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Keycloak Logout Handler
 * Performs Keycloak end-session logout flow matching PHP OIDCHandler::logout()
 * Redirects to Keycloak's end-session endpoint with id_token_hint and post_logout_redirect_uri
 */
@Component
public class KeycloakLogoutHandler implements LogoutHandler {

    private final KeycloakOidcConfig oidcConfig;

    public KeycloakLogoutHandler(KeycloakOidcConfig oidcConfig) {
        this.oidcConfig = oidcConfig;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, 
                       Authentication authentication) {
        // Session invalidation is handled by Spring Security's default logout
        // The Keycloak RP-initiated logout is handled via OidcClientInitiatedLogoutSuccessHandler
        // configured in SecurityConfig. This handler can perform additional cleanup if needed.
        
        request.getSession().removeAttribute("oidc_uid");
        request.getSession().removeAttribute("oidc_email");
        request.getSession().removeAttribute("oidc_employee_type");
        request.getSession().removeAttribute("oidc_name_with_initials");
        request.getSession().removeAttribute("login_time");
        request.getSession().removeAttribute("csrf_token");
    }
}
