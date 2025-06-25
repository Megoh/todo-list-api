package com.dominik.todolist.service;

import com.dominik.todolist.dto.RegisterRequest;
import com.dominik.todolist.exception.EmailAlreadyExistsException;
import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("registerUser - should save a new user with encoded password")
    void registerUser_shouldSaveNewUser() {
        final var request = new RegisterRequest("Test User", "test@example.com", "password123");
        when(appUserRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword123");

        userService.registerUser(request);

        final var userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());

        final var savedUser = userCaptor.getValue();
        assertEquals(request.name(), savedUser.getName());
        assertEquals(request.email(), savedUser.getEmail());
        assertEquals("hashedPassword123", savedUser.getPassword());
    }

    @Test
    @DisplayName("registerUser - should throw exception if email already exists")
    void registerUser_shouldThrowException_whenEmailExists() {
        final var request = new RegisterRequest("Another User", "test@example.com", "password456");
        when(appUserRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.registerUser(request);
        });

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("loadUserByUsername - should return UserDetails when user is found")
    void loadUserByUsername_shouldReturnUserDetails_whenUserFound() {
        final var email = "found@example.com";
        final var mockUser = AppUser.builder()
                .email(email)
                .password("encodedPassword")
                .build();
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserDetails userDetails = userService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
    }

    @Test
    @DisplayName("loadUserByUsername - should throw exception when user is not found")
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
        final var email = "notfound@example.com";
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(email);
        });
    }
}