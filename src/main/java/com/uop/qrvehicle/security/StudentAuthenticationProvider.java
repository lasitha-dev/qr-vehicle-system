package com.uop.qrvehicle.security;

import com.uop.qrvehicle.model.User;
import com.uop.qrvehicle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Authentication provider for student login.
 * Replicates PHP logincheck.php student authentication flow:
 * 
 * 1. Validates Reg_No (username) + NIC (password) against studdb:
 *    SELECT s.Reg_No, s.NIC, sb.L_Name, s.Status
 *    FROM stud s INNER JOIN studbasic sb ON s.NIC = sb.NIC
 *    WHERE s.Reg_No = ? AND s.NIC = ? AND s.Status = 'REGISTERED'
 *
 * 2. On success, auto-creates or updates user in vehicle_qr_db.user table
 *    with utype='entry' (matching PHP behavior)
 *
 * 3. Returns authenticated token so Spring Security proceeds
 *
 * This provider is tried AFTER the default DaoAuthenticationProvider
 * (which checks the user table directly). This means:
 *   - Admin users: matched first by DaoAuthenticationProvider
 *   - Students: fall through to this provider when not found in user table
 */
@Component
public class StudentAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(StudentAuthenticationProvider.class);

    private final JdbcTemplate studDbJdbcTemplate;
    private final UserRepository userRepository;

    public StudentAuthenticationProvider(
            @Qualifier("studDbJdbcTemplate") JdbcTemplate studDbJdbcTemplate,
            UserRepository userRepository) {
        this.studDbJdbcTemplate = studDbJdbcTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();           // Reg_No (e.g., M/24/001)
        String password = authentication.getCredentials().toString(); // NIC (e.g., 200012345678)

        // Only attempt student auth for registration-number-like usernames
        // Student Reg_No pattern: LETTERS/DIGITS/DIGITS (e.g., M/24/001, E/23/045, AHS/22/010)
        if (!username.matches("^[A-Za-z]+/\\d+/.*")) {
            // Not a student Reg_No format, skip this provider
            return null;
        }

        log.debug("Attempting student authentication for Reg_No={}", username);

        try {
            // Query studdb: validate Reg_No + NIC + Status
            // Mirrors PHP: SELECT s.Reg_No, s.NIC, sb.L_Name, s.Status
            //              FROM stud s INNER JOIN studbasic sb ON s.NIC = sb.NIC
            //              WHERE s.Reg_No = ? AND s.NIC = ? AND s.Status = 'REGISTERED'
            String sql = """
                SELECT s.Reg_No, s.NIC, sb.L_Name, s.Status
                FROM stud s
                INNER JOIN studbasic sb ON s.NIC = sb.NIC
                WHERE s.Reg_No = ? AND s.NIC = ? AND s.Status = 'REGISTERED'
                LIMIT 1
                """;

            Map<String, Object> studentRow;
            try {
                studentRow = studDbJdbcTemplate.queryForMap(sql, username, password);
            } catch (EmptyResultDataAccessException e) {
                // Student not found in studdb — let other providers handle it or fail
                log.debug("Student not found in studdb for Reg_No={}", username);
                throw new BadCredentialsException("Student not found or not registered: " + username);
            }

            String regNo = (String) studentRow.get("Reg_No");
            String nic = (String) studentRow.get("NIC");
            String lastName = (String) studentRow.get("L_Name");

            log.info("Student authenticated via studdb: Reg_No={}, Name={}", regNo, lastName);

            // Auto-create or update user in vehicle_qr_db.user table
            // Mirrors PHP: INSERT INTO user (username, password, utype, FullName, Create-Date, LastLogin)
            //              VALUES (?, ?, 'entry', ?, CURDATE(), NOW())
            //              or UPDATE user SET password=?, FullName=?, LastLogin=NOW() WHERE username=?
            User user = userRepository.findByUsername(regNo).orElse(null);
            if (user == null) {
                user = new User();
                user.setUsername(regNo);
                user.setPassword(nic);
                user.setUserType("entry");
                user.setFullName(lastName);
                user.setCreateDate(java.time.LocalDate.now().toString());
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                log.info("Created new user record for student: {}", regNo);
            } else {
                user.setPassword(nic);
                user.setFullName(lastName);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                log.info("Updated user record for student: {}", regNo);
            }

            // Build authenticated token with ROLE_ENTRY (matching PHP utype='entry')
            CustomUserDetails userDetails = new CustomUserDetails(user);
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    password,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ENTRY"))
            );

        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during student authentication for {}: {}", username, e.getMessage(), e);
            throw new BadCredentialsException("Authentication failed for student: " + username, e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
