package com.enoch.leathercraft.superadmin.controller;

import com.enoch.leathercraft.dto.ContactMessageAdminDto;
import com.enoch.leathercraft.services.ContactMessageAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/contact-messages")
@RequiredArgsConstructor
public class SuperAdminContactMessageController {

    private final ContactMessageAdminService service;

    @GetMapping
    public List<ContactMessageAdminDto> getAll() {
        return service.getAll();
    }

    @PatchMapping("/{id}/handled")
    public ContactMessageAdminDto setHandled(
            @PathVariable Long id,
            @RequestParam boolean value,
            Authentication auth
    ) {
        return service.setHandled(id, value, auth);
    }
}
