package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ContactMessageAdminDto;
import com.enoch.leathercraft.entities.ContactMessage;

import com.enoch.leathercraft.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageAdminService {

    private final ContactMessageRepository repo;

    public List<ContactMessageAdminDto> getAll() {
        return repo.findAll()
                .stream()
                .sorted(Comparator.comparing(ContactMessage::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ContactMessageAdminDto setHandled(Long id, boolean value, Authentication auth) {
        ContactMessage msg = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ContactMessage introuvable"));

        msg.setHandled(value);

        if (value) {
            msg.setHandledAt(LocalDateTime.now());
            msg.setHandledBy(auth != null ? auth.getName() : "unknown");
        } else {
            msg.setHandledAt(null);
            msg.setHandledBy(null);
        }

        return toDto(msg);
    }

    private ContactMessageAdminDto toDto(ContactMessage m) {
        return ContactMessageAdminDto.builder()
                .id(m.getId())
                .name(m.getName())
                .email(m.getEmail())
                .message(m.getMessage())
                .createdAt(m.getCreatedAt())
                .handled(m.isHandled())
                .handledAt(m.getHandledAt())
                .handledBy(m.getHandledBy())
                .build();
    }
}
