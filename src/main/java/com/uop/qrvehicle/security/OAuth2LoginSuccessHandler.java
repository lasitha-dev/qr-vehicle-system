package com.uop.qrvehicle.security;

import com.uop.qrvehicle.model.User;
import com.uop.qrvehicle.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * OAuth2 Login Success Handler
 * Handles successful Google OAuth2 login and creates/updates user in database
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    // Allowed email domains for University of Peradeniya
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
        "pdn.ac.lk", "agri.pdn.ac.lk", "ahs.pdn.ac.lk", "alumni.pdn.ac.lk",
        "arts.pdn.ac.lk", "cdce.pdn.ac.lk", "ceit.pdn.ac.lk", "dental.pdn.ac.lk",
        "engmis.pdn.ac.lk", "gs.pdn.ac.lk", "med.pdn.ac.lk", "mgt.pdn.ac.lk",
        "pgims.pdn.ac.lk", "pgis.pdn.ac.lk", "sci.pdn.ac.lk", "sciims.pdn.ac.lk",
        "sites.pdn.ac.lk", "soc.pdn.ac.lk", "vet.pdn.ac.lk"
    );

    public OAuth2LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        Authentication authentication) 
                                        throws IOException, ServletException {
        
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = token.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Validate email domain
        if (!isAllowedDomain(email)) {
            response.sendRedirect("/login?error=domain");
            return;
        }

        // Find or create user
        User user = userRepository.findByUsername(email)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(email);
                newUser.setFullName(name);
                newUser.setPassword(""); // No password for OAuth users
                newUser.setUserType("GoogleUser");
                newUser.setCreateDate(java.time.LocalDate.now().toString());
                return newUser;
            });

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Redirect to dashboard
        response.sendRedirect("/dashboard");
    }

    private boolean isAllowedDomain(String email) {
        if (email == null) return false;
        
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return ALLOWED_DOMAINS.stream()
            .anyMatch(allowed -> domain.equals(allowed) || domain.endsWith("." + allowed));
    }
}
