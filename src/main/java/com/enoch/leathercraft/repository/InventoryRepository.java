package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {}