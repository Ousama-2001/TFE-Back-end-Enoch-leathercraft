package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
