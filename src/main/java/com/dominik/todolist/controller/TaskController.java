package com.dominik.todolist.controller;

import com.dominik.todolist.dto.CreateTaskRequest;
import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.dto.TaskResponse;
import com.dominik.todolist.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest taskRequest) {
        TaskResponse createdTaskResponse = taskService.createTask(taskRequest, getCurrentUserEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTaskResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        TaskResponse taskResponse = taskService.getTaskByIdAndAppUser(id, getCurrentUserEmail());
        return ResponseEntity.ok(taskResponse);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasksForCurrentAppUser() {
        List<TaskResponse> tasksResponse = taskService.getAllTasksForAppUser(getCurrentUserEmail());
        return ResponseEntity.ok(tasksResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody TaskRequest taskRequest) {
        TaskResponse updatedTaskResponse = taskService.updateTask(id, taskRequest, getCurrentUserEmail());
        return ResponseEntity.ok(updatedTaskResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.deleteTask(id, getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to get the email of the currently authenticated user.
     *
     * @return The email (username) of the authenticated user.
     */
    private String getCurrentUserEmail() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();

        // It's safe to cast as our JwtAuthFilter and UserDetailsService ensure it's a UserDetails object
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userDetails.getUsername();
    }
}
