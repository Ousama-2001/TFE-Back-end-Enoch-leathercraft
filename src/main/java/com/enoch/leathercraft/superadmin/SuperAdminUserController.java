// src/main/java/com/enoch/leathercraft/controller/SuperAdminUserController.java
package com.enoch.leathercraft.superadmin;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.superadmin.SuperAdminUserService;
import com.enoch.leathercraft.superadmin.dto.AdminUserSummaryDto;
import com.enoch.leathercraft.superadmin.dto.UpdateUserRoleRequest;
import com.enoch.leathercraft.superadmin.dto.UserAdminDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/users")
@RequiredArgsConstructor
public class SuperAdminUserController {

    private final SuperAdminUserService superAdminUserService;

    /**
     * GET /api/super-admin/users
     * Liste tous les utilisateurs non supprimés
     */
    @GetMapping
    public ResponseEntity<List<UserAdminDto>> getAllUsers() {
        List<UserAdminDto> users = superAdminUserService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * PATCH /api/super-admin/users/{id}/role
     * Body JSON : { "role": "ADMIN" }
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserAdminDto> changeRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        String currentEmail = authentication.getName();
        Role newRole = Role.valueOf(request.role().toUpperCase());

        UserAdminDto dto = superAdminUserService.updateUserRole(id, newRole, currentEmail);
        return ResponseEntity.ok(dto);
    }

    /**
     * DELETE /api/super-admin/users/{id}
     * -> soft delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String currentEmail = authentication.getName();
        superAdminUserService.softDeleteUser(id, currentEmail);
        return ResponseEntity.noContent().build();
    }
}
