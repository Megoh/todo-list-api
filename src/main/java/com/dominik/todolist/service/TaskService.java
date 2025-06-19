package com.dominik.todolist.service;

import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.dto.TaskResponse;
import com.dominik.todolist.exception.TaskNotFoundException;
import com.dominik.todolist.exception.UserNotFoundException;
import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import com.dominik.todolist.repository.TaskRepository;
import com.dominik.todolist.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final AppUserRepository appUserRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository, AppUserRepository appUserRepository) {
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Creates a new task for the given user.
     *
     * @param taskRequest DTO containing title and description
     * @param userEmail   Email of the user creating the task
     * @return The created TaskResponse
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    public TaskResponse createTask(TaskRequest taskRequest, String userEmail) {
        final var appUser = findUserByEmail(userEmail);

        return mapToTaskResponse(taskRepository.save(
                Task.builder()
                        .title(taskRequest.title())
                        .description(taskRequest.description())
                        .status(TaskStatus.TO_DO)
                        .appUser(appUser)
                        .build()
        ));
    }

    /**
     * Retrieves all tasks for a specific user.
     *
     * @param userEmail Email of the user
     * @return List of TaskResponse objects
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasksForAppUser(String userEmail) {
        final var appUser = findUserByEmail(userEmail);

        return taskRepository.findByAppUserId(appUser.getId())
                .stream()
                .map(this::mapToTaskResponse)
                .toList();
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getAppUser().getId(),
                task.getAppUser().getEmail()
        );
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskByIdAndAppUser(Long taskId, String userEmail) {
        final var task = getAndVerifyTaskOwner(taskId, userEmail);
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest, String userEmail) {
        final var task = getAndVerifyTaskOwner(taskId, userEmail);

        if (taskRequest.title() != null && !taskRequest.title().isBlank()) {
            task.setTitle(taskRequest.title());
        }

        if (taskRequest.description() != null && !taskRequest.description().isBlank()) {
            task.setDescription(taskRequest.description());
        }

        if (taskRequest.status() != null) {
            task.setStatus(taskRequest.status());
        }

        return mapToTaskResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId, String userEmail) {
        final var task = getAndVerifyTaskOwner(taskId, userEmail);
        taskRepository.delete(task);
    }

    private Task getAndVerifyTaskOwner(Long taskId, String userEmail) {
        final var user = findUserByEmail(userEmail);

        final var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + taskId));

        if (!task.getAppUser().getId().equals(user.getId())) {
            // We throw TaskNotFoundException to avoid revealing that the task exists but belongs to someone else.
            throw new TaskNotFoundException("Task not found with id: " + taskId);
        }

        return task;
    }

    private AppUser findUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
}