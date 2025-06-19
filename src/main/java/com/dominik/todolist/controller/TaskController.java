package com.dominik.todolist.controller;

import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.dto.TaskResponse;
import com.dominik.todolist.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest taskRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        String appUserEmail = userDetails.getUsername();

        TaskResponse createdTaskResponse = taskService.createTask(taskRequest, appUserEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTaskResponse);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasksForCurrentAppUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        String appUserEmail = userDetails.getUsername();

        List<TaskResponse> tasksResponse = taskService.getAllTasksForAppUser(appUserEmail);
        return ResponseEntity.ok(tasksResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody TaskRequest taskRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        String appUserEmail = userDetails.getUsername();

        TaskResponse updatedTaskResponse = taskService.updateTask(id, taskRequest, appUserEmail);
        return ResponseEntity.ok(updatedTaskResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String appUserEmail = userDetails.getUsername();

        taskService.deleteTask(id, appUserEmail);
        return ResponseEntity.noContent().build();
    }
}
