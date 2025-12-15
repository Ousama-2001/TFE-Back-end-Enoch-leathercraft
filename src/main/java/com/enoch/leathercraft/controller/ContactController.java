package com.enoch.leathercraft.controllers;

import com.enoch.leathercraft.dto.ContactRequest;
import com.enoch.leathercraft.services.ContactRateLimitService;
import com.enoch.leathercraft.services.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final ContactRateLimitService rateLimitService;

    @PostMapping
    public ResponseEntity<?> send(
            @Valid @RequestBody ContactRequest req,
            HttpServletRequest request
    ) {
        String ip = extractClientIp(request);

        if (!rateLimitService.isAllowed(ip)) {
            return ResponseEntity.status(429)
                    .body("Veuillez attendre avant d’envoyer un nouveau message.");
        }

        contactService.handle(req);
        return ResponseEntity.ok().build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
