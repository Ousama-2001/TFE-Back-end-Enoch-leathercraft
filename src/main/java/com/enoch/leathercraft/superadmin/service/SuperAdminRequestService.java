package com.enoch.leathercraft.superadmin.service;

import com.enoch.leathercraft.superadmin.ReactivationRequest;
import com.enoch.leathercraft.superadmin.repository.ReactivationRequestRepository;
import com.enoch.leathercraft.superadmin.dto.ReactivationRequestDto;
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
