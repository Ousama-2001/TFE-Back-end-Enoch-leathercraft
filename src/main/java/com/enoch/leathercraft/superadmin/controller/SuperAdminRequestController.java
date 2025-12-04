package com.enoch.leathercraft.superadmin.controller;

import com.enoch.leathercraft.superadmin.dto.ReactivationRequestDto;
import com.enoch.leathercraft.superadmin.service.SuperAdminRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/reactivation-requests")
@RequiredArgsConstructor
public class SuperAdminRequestController {

    private final SuperAdminRequestService superAdminRequestService;

    @GetMapping
    public ResponseEntity<List<ReactivationRequestDto>> getAll() {
        List<ReactivationRequestDto> list = superAdminRequestService.findAllReactivationRequests();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/handled")
    public ResponseEntity<ReactivationRequestDto> updateHandled(
            @PathVariable Long id,
            @RequestParam("value") boolean value,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        ReactivationRequestDto dto = superAdminRequestService.updateHandled(id, value, adminEmail);
        return ResponseEntity.ok(dto);
    }
}
