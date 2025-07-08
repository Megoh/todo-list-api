package com.dominik.todolist.dto;

import com.dominik.todolist.model.TaskStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long userId,
        String userEmail
) {
}
