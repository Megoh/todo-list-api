package com.dominik.todolist.service.auth;

import com.dominik.todolist.model.AppUser;

public interface AuthenticatedUserService {
    AppUser getAuthenticatedUser();
}