package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Client : Ses commandes
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String email);

    // Admin : Toutes les commandes (du plus récent au plus vieux)
    List<Order> findAllByOrderByCreatedAtDesc();
}