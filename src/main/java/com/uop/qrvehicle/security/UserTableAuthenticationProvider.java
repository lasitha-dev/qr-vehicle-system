package com.uop.qrvehicle.security;

import com.uop.qrvehicle.model.User;
import com.uop.qrvehicle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Custom AuthenticationProvider for form-based login.
 * 
 * Replicates PHP logincheck.php Tier 1 behavior:
 *   SELECT username, password, utype, FullName, `Create-Date`
 *   FROM user
 *   WHERE username = ? AND password = ?
 *     AND utype IN ('Admin', 'viewer', 'searcher', 'certifier', 'entry')
 * 
 * Key difference from standard DaoAuthenticationProvider:
 * - DaoAuthenticationProvider loads user by username ONLY, then checks password separately
 * - This provider queries by BOTH username AND password together
 * - This allows the same username (e.g., "gsd") to be used by different people
 *   with different passwords, each mapped to a distinct user row
 * 
 * This provider is tried FIRST in the authentication chain.
 * If it fails, StudentAuthenticationProvider is tried next.
 */
@Component
public class UserTableAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(UserTableAuthenticationProvider.class);

    private final UserRepository userRepository;

    public UserTableAuthenticationProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.debug("Attempting user table authentication for username={}", username);

        // Query by username + password, filtered by allowed utypes (mirrors PHP)
        List<User> matchedUsers = userRepository.findByUsernameAndPasswordForLogin(username, password);

        if (matchedUsers.isEmpty()) {
            log.debug("No matching user found in user table for username={}", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (matchedUsers.size() > 1) {
            // Same username + same password matches multiple utypes — ambiguous
            // PHP also rejects this (num_rows === 1)
            log.warn("Ambiguous login: username={} matched {} rows with same password", username, matchedUsers.size());
            throw new BadCredentialsException("Ambiguous credentials — contact administrator");
        }

        User user = matchedUsers.get(0);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User authenticated via user table: username={}, utype={}, fullName={}",
                user.getUsername(), user.getUserType(), user.getFullName());

        // Build authenticated token
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
