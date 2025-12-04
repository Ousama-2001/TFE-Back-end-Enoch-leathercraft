package com.enoch.leathercraft.superadmin.repository;

import com.enoch.leathercraft.superadmin.ReactivationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReactivationRequestRepository extends JpaRepository<ReactivationRequest, Long> {

    boolean existsByEmailAndHandledIsFalse(String email);

    List<ReactivationRequest> findAllByOrderByCreatedAtDesc();
}
