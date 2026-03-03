package com.uop.qrvehicle.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for User entity.
 * Maps to the DB's PRIMARY KEY (username, utype).
 * 
 * This allows the same username (e.g., "gsd") to exist multiple times
 * with different userType values (e.g., "certifier", "viewer") — each
 * representing a different person identified by their unique password.
 */
public class UserId implements Serializable {

    private String username;
    private String userType;

    public UserId() {}

    public UserId(String username, String userType) {
        this.username = username;
        this.userType = userType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(username, userId.username)
            && Objects.equals(userType, userId.userType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, userType);
    }
}
