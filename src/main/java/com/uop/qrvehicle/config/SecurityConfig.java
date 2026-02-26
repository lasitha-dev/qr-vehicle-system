package com.uop.qrvehicle.config;

import com.uop.qrvehicle.security.CustomUserDetailsService;
import com.uop.qrvehicle.security.KeycloakLogoutHandler;
import com.uop.qrvehicle.security.KeycloakOidcSuccessHandler;
import com.uop.qrvehicle.security.OAuth2LoginSuccessHandler;
import com.uop.qrvehicle.security.StudentAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
    private final StudentAuthenticationProvider studentAuthenticationProvider;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                         OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                         KeycloakOidcSuccessHandler keycloakOidcSuccessHandler,
                         KeycloakLogoutHandler keycloakLogoutHandler,
                         ClientRegistrationRepository clientRegistrationRepository,
                         StudentAuthenticationProvider studentAuthenticationProvider) {
        this.userDetailsService = userDetailsService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.keycloakOidcSuccessHandler = keycloakOidcSuccessHandler;
        this.keycloakLogoutHandler = keycloakLogoutHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.studentAuthenticationProvider = studentAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                // Public resources
                .requestMatchers("/", "/login", "/error", "/error/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/dashboard/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER", "SEARCHER", "STUDENT", "USER", "GOOGLEUSER")

                // Admin-only routes
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/qr/**").hasRole("ADMIN")
                .requestMatchers("/vehicle/pending", "/vehicle/approve", "/vehicle/reject").hasRole("ADMIN")
                .requestMatchers("/vehicle/update", "/vehicle/delete", "/vehicle/certificate/delete").hasRole("ADMIN")

                // Admin + Entry: vehicle insert/add
                .requestMatchers("/vehicle/insert", "/vehicle/add").hasAnyRole("ADMIN", "ENTRY")

                // Admin + Entry + Viewer: person/staff/student views
                .requestMatchers("/staff/**", "/student/**", "/view/detail").hasAnyRole("ADMIN", "ENTRY", "VIEWER")
                .requestMatchers("/idcard/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER")

                // Viewer: image management
                .requestMatchers("/view/images/**").hasAnyRole("ADMIN", "VIEWER")
                .requestMatchers("/uploads/images/**").hasAnyRole("ADMIN", "VIEWER")
                .requestMatchers("/api/persons/list", "/api/students/**").hasAnyRole("ADMIN", "VIEWER")

                // Searcher + Admin: vehicle search
                .requestMatchers("/vehicle/search", "/vehicle/scanner").hasAnyRole("ADMIN", "SEARCHER")

                // Person search (public QR scan landing + admin/searcher)
                .requestMatchers("/search/**").hasAnyRole("ADMIN", "SEARCHER")

                // API endpoints used by views
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER", "SEARCHER")

                // All other routes require authentication
                .anyRequest().authenticated()
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
        // Build a ProviderManager with:
        // 1. DaoAuthenticationProvider (user table lookup - admin/existing users)
        // 2. StudentAuthenticationProvider (studdb lookup - student Reg_No + NIC)
        // This mirrors the PHP 3-tier login flow in logincheck.php
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(userDetailsService);
        daoProvider.setPasswordEncoder(passwordEncoder());
        daoProvider.setHideUserNotFoundExceptions(true);

        return new ProviderManager(java.util.List.of(
                daoProvider,
                studentAuthenticationProvider
        ));
    }
}
