package com.enoch.leathercraft.services;

import com.enoch.leathercraft.entities.*;

import com.enoch.leathercraft.repository.AdminRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRequestService {

    private final AdminRequestRepository repo;

    public List<AdminRequest> getAll() {
        return repo.findAll();
    }

    public AdminRequest markInProgress(Long id, String adminEmail) {
        AdminRequest r = repo.findById(id).orElseThrow();
        r.setStatus(RequestStatus.IN_PROGRESS);
        return repo.save(r);
    }

    public AdminRequest markResolved(Long id, String adminEmail) {
        AdminRequest r = repo.findById(id).orElseThrow();
        r.setStatus(RequestStatus.RESOLVED);
        r.setResolvedAt(Instant.now());
        r.setResolvedByEmail(adminEmail);
        return repo.save(r);
    }

    public AdminRequest createContactRequest(String email, String subject, String message) {
        AdminRequest r = new AdminRequest();
        r.setType(RequestType.CONTACT);
        r.setEmail(email);
        r.setSubject(subject);
        r.setMessage(message);
        return repo.save(r);
    }

    public AdminRequest createReactivationRequest(String email) {
        AdminRequest r = new AdminRequest();
        r.setType(RequestType.REACTIVATION);
        r.setEmail(email);
        r.setMessage("Demande de réactivation de compte");
        return repo.save(r);
    }
}
