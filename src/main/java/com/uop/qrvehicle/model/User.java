package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Entity - Maps to the 'user' table
 * Stores system users for authentication
 * Composite primary key: (username, utype)
 * 
 * This allows the same username (e.g., "gsd") to have multiple rows
 * with different userType values. Different people use different passwords
 * to log in under the same username — the password identifies the person.
 * This mirrors the PHP logincheck.php behavior.
 */
@Entity
@Table(name = "user")
@IdClass(UserId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "FullName")
    private String fullName;

    @Id
    @Column(name = "utype")
    private String userType; // admin, entry, viewer, searcher, certifier, GoogleUser

    @Column(name = "Create-Date")
    private String createDate; // Stored as varchar in DB

    @Column(name = "LastLogin")
    private LocalDateTime lastLogin;

    // Helper method to get role with ROLE_ prefix for Spring Security
    public String getRole() {
        if (userType == null) {
            return "ROLE_USER";
        }

        String normalized = userType.trim();
        if (normalized.isEmpty()) {
            return "ROLE_USER";
        }

        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }

        return "ROLE_" + normalized.toUpperCase().replace(' ', '_');
    }

    // Check if user is admin
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(userType);
    }

    // Check if user is a certifier
    public boolean isCertifier() {
        return "certifier".equalsIgnoreCase(userType);
    }

    // Check if user logged in via Google
    public boolean isGoogleUser() {
        return "GoogleUser".equalsIgnoreCase(userType);
    }
}
