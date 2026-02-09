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
                .requestMatchers("/api/keepalive/**").authenticated()
                
                // Admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/vehicle/insert/**", "/vehicle/delete/**").hasRole("ADMIN")
                .requestMatchers("/qr/generate/**").hasRole("ADMIN")
                
                // Admin and Entry
                .requestMatchers("/vehicle/**").hasAnyRole("ADMIN", "ENTRY")
                .requestMatchers("/certificate/**").hasAnyRole("ADMIN", "ENTRY")
                
                // Self-service vehicle registration for all authenticated users
                .requestMatchers("/my/vehicle/**").authenticated()
                
                // Viewer access
                .requestMatchers("/view/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER")
                
                // Searcher access
                .requestMatchers("/search/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER", "SEARCHER")
                
                // Student details for admin/entry/viewer
                .requestMatchers("/student/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER")
                
                // Dashboard for all authenticated users
                .requestMatchers("/dashboard/**").authenticated()
                
                // All other requests require authentication
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
                .logoutUrl("/logout")
                .addLogoutHandler(keycloakLogoutHandler)
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Disable CSRF for API keepalive endpoint (uses its own CSRF scheme)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/keepalive", "/api/keepalive/**")
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * OIDC RP-Initiated Logout handler.
     * Redirects to Keycloak end-session endpoint on logout.
     * Falls back to /login?logout=true for form-login users.
     */
    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}/login?logout=true");
        return handler;
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
