package com.techouts.repository;

import com.techouts.entity.User;
import com.techouts.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);
    
    Optional<User> findByUsername(String username);

    long countByRole(UserRole role);
}
