package com.dominik.todolist.service;

import com.dominik.todolist.dto.CreateTaskRequest;
import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.exception.TaskNotFoundException;
import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import com.dominik.todolist.repository.TaskRepository;
import com.dominik.todolist.service.auth.AuthenticatedUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    private static final String TEST_USER_EMAIL = "test.user@example.com";
    private static final Long TEST_USER_ID = 1L;
    private static final TaskStatus TEST_TASK_STATUS = TaskStatus.TO_DO;
    private static final Long TEST_TASK_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private TaskService taskService;

    @Test
    @DisplayName("createTask - should create and save a new task")
    void createTask_shouldCreateAndSaveNewTask() {
        final var userEmail = TEST_USER_EMAIL;
        final var request = new CreateTaskRequest("New Task", "Description");

        final var mockUser = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .name("Test User")
                .build();

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(mockUser);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            final var taskToSave = invocation.getArgument(0, Task.class);
            taskToSave.setId(99L);
            return taskToSave;
        });

        final var result = taskService.createTask(request);

        assertNotNull(result);
        assertEquals("New Task", result.title());
        assertEquals(TaskStatus.TO_DO, result.status());
        assertEquals(userEmail, result.userEmail());
        assertEquals(99L, result.id());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("getAllTasksForCurrentUser - should return all tasks for current user")
    void getAllTasksForCurrentUser_shouldReturnAllTasks() {
        final var userEmail = TEST_USER_EMAIL;
        final var mockUser = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .name("Test User")
                .build();

        final var mockTasks = List.of(
                Task.builder()
                        .id(101L)
                        .title("Task 1")
                        .appUser(mockUser)
                        .build(),
                Task.builder()
                        .id(102L)
                        .title("Task 2")
                        .appUser(mockUser)
                        .build()
        );

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(mockUser);

        when(taskRepository.findByAppUser_Id(mockUser.getId())).thenReturn(mockTasks);

        final var results = taskService.getAllTasksForCurrentUser();

        assertEquals(2, results.size());
        assertEquals("Task 1", results.get(0).title());
        assertEquals(102L, results.get(1).id());

        verify(authenticatedUserService).getAuthenticatedUser();
        verify(taskRepository).findByAppUser_Id(mockUser.getId());
    }

    @Test
    @DisplayName("getTaskByIdAndAppUser - should throw exception when task not found")
    void getTaskByIdAndAppUser_shouldThrowException_whenTaskNotFound() {
        final var userEmail = TEST_USER_EMAIL;
        final var nonExistentTaskId = 123L;
        final var mockUser = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .build();

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskByIdAndAppUser(nonExistentTaskId));

        verify(taskRepository).findById(nonExistentTaskId);
        verify(authenticatedUserService).getAuthenticatedUser();
    }

    @Test
    @DisplayName("getTaskByIdAndAppUser - should return task when found and owned by user")
    void getTaskByIdAndAppUser_shouldReturnTask_whenFoundAndOwnedByUser() {
        final var userEmail = TEST_USER_EMAIL;
        final var taskId = TEST_TASK_ID;
        final var mockUser = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .build();
        final var mockTask = Task.builder()
                .id(taskId)
                .title("My Task")
                .appUser(mockUser)
                .build();

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));

        final var result = taskService.getTaskByIdAndAppUser(taskId);

        assertNotNull(result);
        assertEquals("My Task", result.title());
        assertEquals(userEmail, result.userEmail());
        verify(taskRepository).findById(taskId);
    }

    @Test
    @DisplayName("getTaskByIdAndAppUser - should throw exception for another user's task")
    void getTaskByIdAndAppUser_shouldThrowException_forAnotherUsersTask() {
        final var userEmail = "user.a@example.com";
        final var taskId = 2L;
        final var userA = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .build();
        final var userB = AppUser.builder()
                .id(2L)
                .email("user.b@example.com")
                .build();
        final var taskOfUserB = Task.builder()
                .id(taskId)
                .title("User B's Task")
                .appUser(userB)
                .build();

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(userA);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(taskOfUserB));

        assertThrows(TaskNotFoundException.class, () ->
                        taskService.getTaskByIdAndAppUser(taskId),
                "Should throw TaskNotFoundException to prevent information leakage");

        verify(taskRepository).findById(taskId);
    }

    @Test
    @DisplayName("updateTask - should update task fields without an explicit save call")
    void updateTask_shouldUpdateTaskFields() {
        final var userEmail = TEST_USER_EMAIL;
        final var taskId = TEST_TASK_ID;
        final var mockUser = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .build();
        final var existingTask = Task.builder()
                .id(taskId)
                .title("Old Title")
                .description("Old Description")
                .status(TaskStatus.TO_DO)
                .appUser(mockUser)
                .build();

        final var updateRequest = new TaskRequest("New Title", "New Description", TaskStatus.DONE);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        final var result = taskService.updateTask(taskId, updateRequest);

        assertNotNull(result);
        assertEquals("New Title", result.title());
        assertEquals("New Description", result.description());
        assertEquals(TaskStatus.DONE, result.status());

        verify(taskRepository).findById(taskId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("deleteTask - should call delete on the repository")
    void deleteTask_shouldCallDelete() {
        final var userEmail = TEST_USER_EMAIL;
        final var taskId = TEST_TASK_ID;
        final var mockUser = AppUser.builder()
                .id(TEST_USER_ID)
                .email(userEmail)
                .build();
        final var existingTask = Task.builder().id(taskId).appUser(mockUser).build();

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        assertDoesNotThrow(() -> taskService.deleteTask(taskId));

        verify(taskRepository).delete(existingTask);
    }
}