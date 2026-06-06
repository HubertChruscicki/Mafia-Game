package com.mafia.authservice.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.authservice.dto.UpdateEmailRequest;
import com.mafia.authservice.dto.UpdateUsernameRequest;
import com.mafia.authservice.dto.UserInfoResponse;
import com.mafia.authservice.dto.UserResponse;
import com.mafia.authservice.exception.GlobalExceptionHandler;
import com.mafia.authservice.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(UserInfoResponse.builder()
                .id(id)
                .username("alice")
                .email("alice@example.com")
                .admin(false)
                .build());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void profileReturnsFullUser() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(UserResponse.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .admin(false)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void searchReturnsUsers() throws Exception {
        when(userService.searchUsers("ali")).thenReturn(List.of(
                UserResponse.builder()
                        .id(UUID.randomUUID())
                        .username("alice")
                        .email("alice@example.com")
                        .admin(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        mockMvc.perform(get("/api/users/search").param("q", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    void updateUsernameDelegatesToService() throws Exception {
        UpdateUsernameRequest request = new UpdateUsernameRequest("new-name");
        when(userService.updateUsername("new-name")).thenReturn(UserInfoResponse.builder()
                .id(UUID.randomUUID())
                .username("new-name")
                .email("alice@example.com")
                .admin(false)
                .build());

        mockMvc.perform(put("/api/users/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new-name"));

        verify(userService).updateUsername("new-name");
    }

    @Test
    void updateEmailDelegatesToService() throws Exception {
        UpdateEmailRequest request = new UpdateEmailRequest("new@example.com");
        when(userService.updateEmail("new@example.com")).thenReturn(UserInfoResponse.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("new@example.com")
                .admin(false)
                .build());

        mockMvc.perform(put("/api/users/me/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }
}
