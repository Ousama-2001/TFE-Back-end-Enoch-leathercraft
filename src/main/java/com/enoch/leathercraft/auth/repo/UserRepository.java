// src/main/java/com/enoch/leathercraft/auth/repo/UserRepository.java
package com.enoch.leathercraft.auth.repo;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Ignore-case (Spring Data génère LOWER(...) automatiquement)
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);

    // Tu peux garder ceux-ci si tu veux, mais évite de les utiliser pour l’auth
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmailAndDeletedFalse(String email);

    // 🔥 récupérer tous les ADMIN / SUPER_ADMIN non supprimés
    List<User> findByRoleInAndDeletedFalse(Collection<Role> roles);
}
