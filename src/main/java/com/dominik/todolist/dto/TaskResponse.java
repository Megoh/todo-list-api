package com.dominik.todolist.dto;

import com.dominik.todolist.model.TaskStatus;

import java.time.LocalDateTime;


public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long userId,
        String userEmail
) {

}
