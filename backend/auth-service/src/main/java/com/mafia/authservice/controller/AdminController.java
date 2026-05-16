package com.mafia.authservice.controller;

import com.mafia.authservice.dto.UpdateAdminFlagRequest;
import com.mafia.authservice.dto.UserResponse;
import com.mafia.authservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative user management (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "List every registered user")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/{id}/admin")
    @Operation(summary = "Grant or revoke admin privileges for a user")
    public ResponseEntity<UserResponse> updateAdminFlag(@PathVariable("id") UUID id,
                                                        @Valid @RequestBody UpdateAdminFlagRequest request) {
        return ResponseEntity.ok(adminService.updateUserAdminFlag(id, request.isAdmin()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a user account")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
