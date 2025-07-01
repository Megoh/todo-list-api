package com.dominik.todolist.service.auth;

import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.repository.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

    private final AppUserRepository appUserRepository;

    public AuthenticatedUserServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AppUser getAuthenticatedUser() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database."));
    }
}