package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalseAndIdNot(String username, Long id);

    boolean existsByEmailAndIsDeletedFalseAndIdNot(String email, Long id);
}
