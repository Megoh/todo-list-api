package com.dominik.todolist.service;

import com.dominik.todolist.dto.CreateTaskRequest;
import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.dto.TaskResponse;
import com.dominik.todolist.exception.TaskNotFoundException;
import com.dominik.todolist.exception.UserNotFoundException;
import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import com.dominik.todolist.repository.TaskRepository;
import com.dominik.todolist.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final AppUserRepository appUserRepository;

    public TaskService(TaskRepository taskRepository, AppUserRepository appUserRepository) {
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Creates a new task for the given user. The task is always created with an
     * initial status of 'TO_DO', regardless of the status provided in the request.
     *
     * @param taskRequest DTO containing the new task's details (title, description, status).
     * @param userEmail   Email of the user for whom the task is being created.
     * @return A TaskResponse DTO representing the newly created task.
     * @throws UserNotFoundException if a user with the given email does not exist.
     */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest taskRequest, String userEmail) {
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

        return taskRepository.findByAppUser_Id(appUser.getId())
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
                .orElseThrow(() -> TaskNotFoundException.withId(taskId));

        if (!task.getAppUser().getId().equals(user.getId())) {
            // We throw TaskNotFoundException to avoid revealing that the task exists but belongs to someone else.
            throw TaskNotFoundException.withId(taskId);
        }

        return task;
    }

    private AppUser findUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> UserNotFoundException.withEmail(email));
    }
}