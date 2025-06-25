package com.dominik.todolist.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    private UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException withEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    public static UserNotFoundException withId(Long id) {
        return new UserNotFoundException("User not found with ID: " + id);
    }
}