package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ContactRequest;
import com.enoch.leathercraft.entities.ContactMessage;

import com.enoch.leathercraft.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository contactRepo;
    private final MailService mailService;

    @Transactional
    public void handle(ContactRequest req) {
        ContactMessage msg = ContactMessage.builder()
                .name(req.name().trim())
                .email(req.email().trim())
                .message(req.message().trim())
                .build();

        contactRepo.save(msg);

        // méthode que tu as ajoutée dans MailService
        mailService.sendContactEmail(msg.getName(), msg.getEmail(), msg.getMessage());
    }
}
