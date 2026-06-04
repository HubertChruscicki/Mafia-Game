package com.mafia.authservice.controller;

import com.mafia.authservice.dto.UpdateEmailRequest;
import com.mafia.authservice.dto.UpdatePasswordRequest;
import com.mafia.authservice.dto.UpdateUsernameRequest;
import com.mafia.authservice.dto.UserInfoResponse;
import com.mafia.authservice.dto.UserResponse;
import com.mafia.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile inspection and self-service updates")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the current user's basic info")
    public ResponseEntity<UserInfoResponse> me() {
        return ResponseEntity.ok(userService.getCurrentUserInfo());
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get the current user's full profile")
    public ResponseEntity<UserResponse> profile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username (case-insensitive)")
    public ResponseEntity<List<UserResponse>> search(@RequestParam(name = "q", required = false) String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single user by identifier")
    public ResponseEntity<UserResponse> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/me/username")
    @Operation(summary = "Update the current user's username")
    public ResponseEntity<UserInfoResponse> updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        return ResponseEntity.ok(userService.updateUsername(request.getNewUsername()));
    }

    @PutMapping("/me/email")
    @Operation(summary = "Update the current user's email")
    public ResponseEntity<UserInfoResponse> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        return ResponseEntity.ok(userService.updateEmail(request.getNewEmail()));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
