package com.uop.qrvehicle.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keepalive Controller
 * Mirrors PHP keepalive.php — provides CSRF-protected session/token keepalive endpoint.
 * - Validates CSRF token from X-CSRF-TOKEN header
 * - Validates request method (POST only)
 * - Validates origin header
 * - Returns TTL of OIDC access token and session status
 */
@RestController
@RequestMapping("/api/keepalive")
public class KeepaliveController {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public KeepaliveController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * GET endpoint to fetch a fresh CSRF token for use in keepalive POST requests.
     * The PHP app stores csrf in $_SESSION and checks it on POST.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute("csrf_token", csrfToken);

        Map<String, String> result = new HashMap<>();
        result.put("csrf", csrfToken);
        return ResponseEntity.ok(result);
    }

    /**
     * POST keepalive — validates CSRF, checks session, returns token TTL.
     * Mirrors PHP keepalive.php behavior:
     * - Checks X-CSRF-TOKEN header against session csrf
     * - Checks Origin header
     * - Returns { ok: true, ttl: seconds }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> keepalive(
            HttpServletRequest request,
            Authentication authentication) {

        Map<String, Object> result = new HashMap<>();

        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            result.put("ok", false);
            result.put("error", "NOT_AUTHENTICATED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        // CSRF validation
        HttpSession session = request.getSession(false);
        if (session == null) {
            result.put("ok", false);
            result.put("error", "NO_SESSION");
            return ResponseEntity.status(440).body(result);
        }

        String sessionCsrf = (String) session.getAttribute("csrf_token");
        String headerCsrf = request.getHeader("X-CSRF-TOKEN");

        if (sessionCsrf == null || sessionCsrf.isEmpty()) {
            result.put("ok", false);
            result.put("error", "NO_CSRF");
            result.put("detail", "No CSRF token in session");
            return ResponseEntity.status(440).body(result);
        }

        if (!sessionCsrf.equals(headerCsrf)) {
            result.put("ok", false);
            result.put("error", "CSRF");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        // Origin validation
        String origin = request.getHeader("Origin");
        String host = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            host += ":" + request.getServerPort();
        }
        if (origin != null && !origin.isEmpty() && !origin.equals(host)) {
            result.put("ok", false);
            result.put("error", "BAD_ORIGIN");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        // Calculate token TTL
        long ttl = 0;
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

            // Try to get expiration from OIDC ID token claims
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                Instant expiration = oidcUser.getExpiresAt();
                if (expiration != null) {
                    ttl = Math.max(0, expiration.getEpochSecond() - Instant.now().getEpochSecond());
                }
            }

            // Also try to get from authorized client's access token
            try {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
                );
                if (client != null && client.getAccessToken() != null 
                        && client.getAccessToken().getExpiresAt() != null) {
                    long accessTtl = Math.max(0, 
                        client.getAccessToken().getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
                    // Use the minimum of id_token and access_token TTL
                    ttl = ttl > 0 ? Math.min(ttl, accessTtl) : accessTtl;
                }
            } catch (Exception e) {
                // Ignore — authorized client may not be available
            }
        }

        // For form-login users, TTL is the remaining session time
        if (ttl == 0) {
            int maxInactive = session.getMaxInactiveInterval();
            long lastAccessed = session.getLastAccessedTime();
            long elapsed = (System.currentTimeMillis() - lastAccessed) / 1000;
            ttl = Math.max(0, maxInactive - elapsed);
        }

        // Generate a fresh CSRF token for the next request
        String newCsrf = UUID.randomUUID().toString();
        session.setAttribute("csrf_token", newCsrf);

        result.put("ok", true);
        result.put("ttl", ttl);
        result.put("csrf", newCsrf);
        return ResponseEntity.ok(result);
    }
}
