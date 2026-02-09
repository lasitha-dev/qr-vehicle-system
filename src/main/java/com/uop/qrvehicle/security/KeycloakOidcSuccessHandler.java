package com.uop.qrvehicle.security;

import com.uop.qrvehicle.model.User;
import com.uop.qrvehicle.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Keycloak OIDC Login Success Handler
 * Handles successful Keycloak login, extracts custom claims (uid, employee_type, name_with_initials),
 * creates/updates user in DB, and stores session attributes.
 * Mirrors PHP OIDCHandler::stashTokens() behavior.
 */
@Component
public class KeycloakOidcSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(KeycloakOidcSuccessHandler.class);

    private final UserRepository userRepository;

    // Allowed email domains for University of Peradeniya (matches PHP config)
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
        "pdn.ac.lk", "agri.pdn.ac.lk", "ahs.pdn.ac.lk", "alumni.pdn.ac.lk",
        "arts.pdn.ac.lk", "cdce.pdn.ac.lk", "ceit.pdn.ac.lk", "dental.pdn.ac.lk",
        "engmis.pdn.ac.lk", "gs.pdn.ac.lk", "med.pdn.ac.lk", "mgt.pdn.ac.lk",
        "pgims.pdn.ac.lk", "pgis.pdn.ac.lk", "sci.pdn.ac.lk", "sciims.pdn.ac.lk",
        "sites.pdn.ac.lk", "soc.pdn.ac.lk", "vet.pdn.ac.lk"
    );

    public KeycloakOidcSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
                                        throws IOException, ServletException {

        String email = null;
        String name = null;
        String uid = null;
        String employeeType = null;
        String nameWithInitials = null;

        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            email = oidcUser.getEmail();
            name = oidcUser.getFullName();
            uid = oidcUser.getClaimAsString("uid");
            employeeType = oidcUser.getClaimAsString("employee_type");
            nameWithInitials = oidcUser.getClaimAsString("name_with_initials");
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            uid = oauth2User.getAttribute("uid");
            employeeType = oauth2User.getAttribute("employee_type");
            nameWithInitials = oauth2User.getAttribute("name_with_initials");
        }

        // Validate email domain
        if (email != null && !isAllowedDomain(email)) {
            log.warn("OIDC login rejected - invalid domain: {}", email);
            response.sendRedirect("/login?error=domain");
            return;
        }

        // Determine username — prefer uid (employee number), fallback to email
        String username = uid != null ? uid : email;
        String displayName = nameWithInitials != null ? nameWithInitials : name;

        // Find or create user
        User user = userRepository.findByUsername(username)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setFullName(displayName);
                newUser.setPassword(""); // No password for OIDC users
                newUser.setUserType("GoogleUser"); // Default type for OIDC users
                newUser.setCreateDate(java.time.LocalDate.now().toString());
                return newUser;
            });

        user.setLastLogin(LocalDateTime.now());
        if (displayName != null) {
            user.setFullName(displayName);
        }
        userRepository.save(user);

        // Store custom claims in session for later use
        request.getSession().setAttribute("oidc_uid", uid);
        request.getSession().setAttribute("oidc_email", email);
        request.getSession().setAttribute("oidc_employee_type", employeeType);
        request.getSession().setAttribute("oidc_name_with_initials", nameWithInitials);
        request.getSession().setAttribute("login_time", System.currentTimeMillis() / 1000);

        log.info("OIDC login successful for: {} (uid: {})", email, uid);

        response.sendRedirect("/dashboard");
    }

    private boolean isAllowedDomain(String email) {
        if (email == null) return false;
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return ALLOWED_DOMAINS.stream()
            .anyMatch(allowed -> domain.equals(allowed) || domain.endsWith("." + allowed));
    }
}
