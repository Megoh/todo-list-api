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

        Object principal = authentication.getPrincipal();
        String email;

        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            throw new IllegalStateException("Cannot get user details from an anonymous user principal.");
        } else {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
        }

        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user '" + email + "' not found in database."));
    }
}