package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Entity - Maps to the 'user' table
 * Stores system users for authentication
 */
@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "FullName")
    private String fullName;

    @Column(name = "utype")
    private String userType; // admin, entry, viewer, searcher, GoogleUser

    @Column(name = "Create-Date")
    private LocalDateTime createDate;

    @Column(name = "LastLogin")
    private LocalDateTime lastLogin;

    // Helper method to get role with ROLE_ prefix for Spring Security
    public String getRole() {
        if (userType == null) return "ROLE_USER";
        return "ROLE_" + userType.toUpperCase().replace(" ", "_");
    }

    // Check if user is admin
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(userType);
    }

    // Check if user logged in via Google
    public boolean isGoogleUser() {
        return "GoogleUser".equalsIgnoreCase(userType);
    }
}
