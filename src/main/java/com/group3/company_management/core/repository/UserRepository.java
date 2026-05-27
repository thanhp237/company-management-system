package com.group3.company_management.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.group3.company_management.core.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find user by username for login
    Optional<User> findByUsername(String username);
    
    // Find user by email
    Optional<User> findByEmail(String email);
    
    // Find user by username, excluding soft-deleted users
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = false")
    Optional<User> findByUsernameAndNotDeleted(@Param("username") String username);
}