package com.dominik.todolist.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TaskNotFoundException extends RuntimeException {
    private TaskNotFoundException(String message) {
        super(message);
    }

    public static TaskNotFoundException withId(Long taskId) {
        return new TaskNotFoundException("Task not found with ID: " + taskId);
    }

    public static TaskNotFoundException forUser(Long taskId, String userEmail) {
        return new TaskNotFoundException("Task with ID " + taskId + " not found or does not belong to user " + userEmail);
    }
}