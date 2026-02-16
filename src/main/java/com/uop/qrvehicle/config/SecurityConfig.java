package com.uop.qrvehicle.config;

import com.uop.qrvehicle.security.CustomUserDetailsService;
import com.uop.qrvehicle.security.KeycloakLogoutHandler;
import com.uop.qrvehicle.security.KeycloakOidcSuccessHandler;
import com.uop.qrvehicle.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Spring Security Configuration
 * Configures form-based login, OAuth2/OIDC Keycloak login, Google OAuth2, and role-based access control
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final KeycloakOidcSuccessHandler keycloakOidcSuccessHandler;
    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                         OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                         KeycloakOidcSuccessHandler keycloakOidcSuccessHandler,
                         KeycloakLogoutHandler keycloakLogoutHandler,
                         ClientRegistrationRepository clientRegistrationRepository) {
        this.userDetailsService = userDetailsService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.keycloakOidcSuccessHandler = keycloakOidcSuccessHandler;
        this.keycloakLogoutHandler = keycloakLogoutHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/", "/login", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/dashboard/**").hasRole("VIEWER")
                // Viewer image management only
                .requestMatchers("/view/images/**").hasRole("VIEWER")
                .requestMatchers("/uploads/images/**").hasRole("VIEWER")
                .requestMatchers("/api/persons/list", "/api/students/**").hasRole("VIEWER")

                // All other routes are disabled in this migration stage
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            // Keycloak OIDC + Google OAuth2 login
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .successHandler(keycloakOidcSuccessHandler)
                .failureUrl("/login?error=oauth")
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/logout", "GET"))
                .addLogoutHandler(keycloakLogoutHandler)
                .logoutSuccessHandler(compositeLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Disable CSRF for API endpoints (REST)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/view/images/**")
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * Composite logout handler that detects the authentication type.
     * - For OIDC users (Keycloak/Google): uses OidcClientInitiatedLogoutSuccessHandler
     * - For form-login users: uses SimpleUrlLogoutSuccessHandler
     */
    @Bean
    public LogoutSuccessHandler compositeLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcHandler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        oidcHandler.setPostLogoutRedirectUri("{baseUrl}/login?logout=true");

        SimpleUrlLogoutSuccessHandler formHandler = new SimpleUrlLogoutSuccessHandler();
        formHandler.setDefaultTargetUrl("/login?logout=true");

        return (request, response, authentication) -> {
            if (authentication != null
                    && authentication.getPrincipal() instanceof OidcUser) {
                oidcHandler.onLogoutSuccess(request, response, authentication);
            } else {
                formHandler.onLogoutSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Note: The existing PHP system stores plain text passwords
        // Using NoOp encoder for compatibility with legacy plaintext passwords
        // TODO: Migrate to BCrypt in production
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
