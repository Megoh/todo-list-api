package com.dominik.todolist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CreateTaskRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @NotBlank(message = "Description cannot be blank")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}