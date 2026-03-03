package com.uop.qrvehicle.repository;

import com.uop.qrvehicle.model.User;
import com.uop.qrvehicle.model.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository - Data access for User entity
 * Uses composite key (username, userType) matching DB's PRIMARY KEY (username, utype)
 */
@Repository
public interface UserRepository extends JpaRepository<User, UserId> {

    /**
     * Find all users with a given username (may return multiple rows for shared usernames).
     */
    List<User> findByUsername(String username);

    /**
     * Find a specific user by username + userType (unique by composite key).
     * Used by OAuth2/Keycloak/Student handlers to find or create specific user records.
     */
    Optional<User> findByUsernameAndUserType(String username, String userType);

    /**
     * Form login query — mirrors PHP logincheck.php:
     *   SELECT ... FROM user WHERE username=? AND password=?
     *     AND utype IN ('Admin','viewer','searcher','certifier','entry')
     * 
     * Returns List because theoretically the same username+password could match
     * multiple utypes. PHP checks num_rows===1; we do the same in the provider.
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.password = :password " +
           "AND LOWER(u.userType) IN ('admin', 'viewer', 'searcher', 'certifier', 'entry')")
    List<User> findByUsernameAndPasswordForLogin(@Param("username") String username,
                                                  @Param("password") String password);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = CURRENT_TIMESTAMP WHERE u.username = :username AND u.userType = :userType")
    void updateLastLogin(@Param("username") String username, @Param("userType") String userType);
}
