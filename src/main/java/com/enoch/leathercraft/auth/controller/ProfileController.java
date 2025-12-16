package com.enoch.leathercraft.auth.controller;

import com.enoch.leathercraft.auth.dto.EmailChangeConfirmRequest;
import com.enoch.leathercraft.auth.dto.EmailChangeRequest;
import com.enoch.leathercraft.auth.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/email-change-request")
    public ResponseEntity<?> requestEmailChange(Authentication auth,
                                                @RequestBody EmailChangeRequest req) {
        String currentEmail = auth.getName(); // email = username spring security
        profileService.requestEmailChange(currentEmail, req);
        return ResponseEntity.ok("EMAIL_CHANGE_LINK_SENT");
    }

    @PostMapping("/email-change-confirm")
    public ResponseEntity<?> confirmEmailChange(@RequestBody EmailChangeConfirmRequest req) {
        profileService.confirmEmailChange(req);
        return ResponseEntity.ok("EMAIL_CHANGED");
    }
}
