package com.uop.qrvehicle.config;

import com.uop.qrvehicle.security.CustomUserDetailsService;
import com.uop.qrvehicle.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration
 * Configures form-based login, OAuth2 Google login, and role-based access control
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService, 
                         OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/", "/login", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                
                // Admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/vehicle/insert/**", "/vehicle/delete/**").hasRole("ADMIN")
                .requestMatchers("/qr/generate/**").hasRole("ADMIN")
                
                // Admin and Entry
                .requestMatchers("/vehicle/**").hasAnyRole("ADMIN", "ENTRY")
                .requestMatchers("/certificate/**").hasAnyRole("ADMIN", "ENTRY")
                
                // Viewer access
                .requestMatchers("/view/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER")
                
                // Searcher access
                .requestMatchers("/search/**").hasAnyRole("ADMIN", "ENTRY", "VIEWER", "SEARCHER")
                
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
            // NOTE: Google OAuth2 login disabled to match PHP implementation
            // Uncomment to re-enable:
            // .oauth2Login(oauth -> oauth
            //     .loginPage("/login")
            //     .successHandler(oAuth2LoginSuccessHandler)
            //     .failureUrl("/login?error=oauth")
            // )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )
            .userDetailsService(userDetailsService);

        return http.build();
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
