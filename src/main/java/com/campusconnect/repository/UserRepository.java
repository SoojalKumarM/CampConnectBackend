package com.campusconnect.repository;

import com.campusconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    boolean existsByWhatsappNumber(String whatsappNumber);
    Optional<User> findByWhatsappNumber(String whatsappNumber);
}
