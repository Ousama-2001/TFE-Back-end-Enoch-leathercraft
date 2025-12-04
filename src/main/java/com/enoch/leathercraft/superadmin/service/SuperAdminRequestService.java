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

    @Transactional
    public void createReactivationRequest(String email) {

        String normalized = email.trim().toLowerCase();

        // 🔥 Bloque les doublons
        if (reactivationRequestRepository.existsByEmailAndHandledIsFalse(normalized)) {
            return;
        }

        ReactivationRequest req = new ReactivationRequest();
        req.setEmail(normalized);
        req.setHandled(false);
        req.setCreatedAt(Instant.now());

        reactivationRequestRepository.save(req);

        // 🔥 ENVOI EMAIL SUPER ADMIN
        mailService.sendReactivationRequestEmailToAdmin(
                normalized,
                "L'utilisateur souhaite réactiver son compte."
        );
    }

    @Transactional(readOnly = true)
    public List<ReactivationRequestDto> findAllReactivationRequests() {
        return reactivationRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReactivationRequestDto::fromEntity)
                .toList();
    }

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
