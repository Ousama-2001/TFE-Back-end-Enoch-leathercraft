// src/main/java/com/enoch/leathercraft/superadmin/service/SuperAdminRequestService.java
package com.enoch.leathercraft.superadmin.service;

import com.enoch.leathercraft.services.MailService;
import com.enoch.leathercraft.superadmin.ReactivationRequest;
import com.enoch.leathercraft.superadmin.dto.ReactivationRequestDto;
import com.enoch.leathercraft.superadmin.repository.ReactivationRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminRequestService {

    private final ReactivationRequestRepository reactivationRequestRepository;
    private final MailService mailService;

    /**
     * @return true si une nouvelle demande est créée, false si une demande non traitée existe déjà.
     */
    @Transactional
    public boolean createReactivationRequest(String email, String message) {
        String normalized = email.trim().toLowerCase();

        boolean exists = reactivationRequestRepository
                .existsByEmailAndHandledIsFalse(normalized);

        if (exists) {
            return false; // déjà une demande ouverte
        }

        ReactivationRequest req = new ReactivationRequest();
        req.setEmail(normalized);
        req.setHandled(false);
        req.setCreatedAt(Instant.now());
        req.setMessage(message);

        reactivationRequestRepository.save(req);

        mailService.sendReactivationRequestEmailToAdmin(normalized, message);
        return true;
    }


    // --------- Lecture pour la page super admin ---------
    @Transactional(readOnly = true)
    public List<ReactivationRequestDto> findAllReactivationRequests() {
        return reactivationRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReactivationRequestDto::fromEntity)
                .toList();
    }

    // --------- Marquer comme traité / non traité ---------
    @Transactional
    public ReactivationRequestDto updateHandled(Long id, boolean value, String adminEmail) {
        ReactivationRequest req = reactivationRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        req.setHandled(value);
        if (value) {
            req.setHandledAt(Instant.now());
            req.setHandledBy(adminEmail);
        } else {
            req.setHandledAt(null);
            req.setHandledBy(null);
        }

        reactivationRequestRepository.save(req);
        return ReactivationRequestDto.fromEntity(req);
    }
}
