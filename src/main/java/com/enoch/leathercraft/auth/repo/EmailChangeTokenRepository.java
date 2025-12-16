// src/main/java/com/enoch/leathercraft/auth/repo/EmailChangeTokenRepository.java
package com.enoch.leathercraft.auth.repo;

import com.enoch.leathercraft.auth.domain.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    Optional<EmailChangeToken> findByToken(String token);
    void deleteByUserId(Long userId);
}
