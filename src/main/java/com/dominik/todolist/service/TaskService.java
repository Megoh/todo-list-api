package com.dominik.todolist.service;

import com.dominik.todolist.dto.CreateTaskRequest;
import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.dto.TaskResponse;
import com.dominik.todolist.exception.TaskNotFoundException;
import com.dominik.todolist.exception.UserNotFoundException;
import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import com.dominik.todolist.repository.TaskRepository;
import com.dominik.todolist.service.auth.AuthenticatedUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public TaskService(TaskRepository taskRepository, AuthenticatedUserService authenticatedUserService) {
        this.taskRepository = taskRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    /**
     * Creates a new task for the given user. The task is always created with an
     * initial status of 'TO_DO', regardless of the status provided in the request.
     *
     * @param taskRequest DTO containing the new task's details (title, description, status).
     * @return A TaskResponse DTO representing the newly created task.
     * @throws UserNotFoundException if a user with the given email does not exist.
     */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest taskRequest) {
        final var appUser = authenticatedUserService.getAuthenticatedUser();

        return mapToTaskResponse(taskRepository.save(
                Task.builder()
                        .title(taskRequest.title())
                        .description(taskRequest.description())
                        .status(TaskStatus.TO_DO)
                        .appUser(appUser)
                        .build()
        ));
    }

    public Page<TaskResponse> getAllTasksForCurrentUser(TaskStatus status, Pageable pageable) {
        final var currentUserId = authenticatedUserService.getAuthenticatedUser().getId();
        Page<Task> taskPage = taskRepository.findByUserIdAndOptionalStatus(currentUserId, status, pageable);
        return taskPage.map(this::mapToTaskResponse);
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
    public TaskResponse getTaskByIdAndAppUser(Long taskId) {
        final var task = getAndVerifyTaskOwner(taskId);
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest) {
        final var task = getAndVerifyTaskOwner(taskId);

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
    public void deleteTask(Long taskId) {
        final var task = getAndVerifyTaskOwner(taskId);
        taskRepository.delete(task);
    }

    private Task getAndVerifyTaskOwner(Long taskId) {
        final var user = authenticatedUserService.getAuthenticatedUser();

        final var task = taskRepository.findById(taskId)
                .orElseThrow(() -> TaskNotFoundException.withId(taskId));

        if (!task.getAppUser().getId().equals(user.getId())) {
            // We throw TaskNotFoundException to avoid revealing that the task exists but belongs to someone else.
            throw TaskNotFoundException.withId(taskId);
        }

        return task;
    }
}