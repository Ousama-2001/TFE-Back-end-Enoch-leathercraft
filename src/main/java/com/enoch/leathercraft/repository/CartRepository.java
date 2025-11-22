package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.entities.Cart;
import com.enoch.leathercraft.entities.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserAndStatus(User user, CartStatus status);
}
