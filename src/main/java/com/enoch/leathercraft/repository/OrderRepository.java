package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Trouver toutes les commandes d'un client
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String email);
}