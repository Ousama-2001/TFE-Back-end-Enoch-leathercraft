package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
