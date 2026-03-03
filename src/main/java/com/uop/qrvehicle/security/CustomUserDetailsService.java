package com.uop.qrvehicle.security;

import com.uop.qrvehicle.model.User;
import com.uop.qrvehicle.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService for Spring Security
 * Loads user from database for authentication
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // findByUsername returns a List since the same username can have multiple rows
        // (different people using different passwords under the same username).
        // This service is a fallback — main form login uses UserTableAuthenticationProvider.
        java.util.List<User> users = userRepository.findByUsername(username);

        if (users.isEmpty()) {
            throw new UsernameNotFoundException(
                "User not found with username: " + username
            );
        }

        // Take the first match (this service is not the primary auth path)
        User user = users.get(0);

        // Update last login for this specific user (by composite key)
        userRepository.updateLastLogin(user.getUsername(), user.getUserType());

        return new CustomUserDetails(user);
    }
}
