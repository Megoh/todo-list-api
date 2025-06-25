package com.dominik.todolist.controller;

import com.dominik.todolist.dto.LoginRequest;
import com.dominik.todolist.dto.RegisterRequest;
import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/auth/register - Success")
    void whenRegisterWithValidData_thenReturns201AndToken() throws Exception {
        final var registerRequest = new RegisterRequest(
                "Name Surname",
                "name.surname@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/register - Email Already Exists")
    void whenRegisterWithExistingEmail_thenReturns409Conflict() throws Exception {
        appUserRepository.save(
                AppUser.builder()
                        .name("Existing User")
                        .email("test.user@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .build()
        );

        final var registerRequest = new RegisterRequest(
                "New User",
                "test.user@example.com",
                "newpassword456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/register - Invalid Data (Blank Name)")
    void whenRegisterWithBlankName_thenReturns400BadRequest() throws Exception {
        final var registerRequest = new RegisterRequest(
                "",
                "new.user@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", allOf(
                        containsString("Name cannot be blank"),
                        containsString("Name must be between 2 and 100 characters")
                )));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid Credentials")
    void whenLoginWithInvalidCredentials_thenReturns401Unauthorized() throws Exception {
        appUserRepository.save(
                AppUser.builder()
                        .name("Test User")
                        .email("test.login.fail@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .build()
        );

        final var loginRequest = new LoginRequest(
                "test.login.fail@example.com",
                "wrong-password"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void whenLoginWithValidCredentials_thenReturns200AndToken() throws Exception {
        appUserRepository.save(
                AppUser.builder()
                        .name("Test User")
                        .email("test.login.success@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .build()
        );

        final var loginRequest = new LoginRequest(
                "test.login.success@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
