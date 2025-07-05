package com.dominik.todolist.controller;

import com.dominik.todolist.dto.CreateTaskRequest;
import com.dominik.todolist.dto.TaskRequest;
import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import com.dominik.todolist.repository.AppUserRepository;
import com.dominik.todolist.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser userA;
    private AppUser userB;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        appUserRepository.deleteAll();

        userA = AppUser.builder()
                .email("user.a@example.com")
                .name("User A")
                .password(passwordEncoder.encode("passwordA"))
                .build();

        userB = AppUser.builder()
                .email("user.b@example.com")
                .name("User B")
                .password(passwordEncoder.encode("passwordB"))
                .build();

        appUserRepository.save(userA);
        appUserRepository.save(userB);
    }

    @Test
    @DisplayName("POST /api/tasks - Success")
    @WithMockUser("test.user@example.com")
    void whenCreateTaskWithValidData_thenReturns201AndTaskResponse() throws Exception {
        appUserRepository.save(
                AppUser.builder()
                        .email("test.user@example.com")
                        .name("Test User")
                        .password(passwordEncoder.encode("password"))
                        .build()
        );

        final var taskRequest = new CreateTaskRequest(
                "New Task Title",
                "A description for the new task."
        );

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("New Task Title")))
                .andExpect(jsonPath("$.status", is("TO_DO")))
                .andExpect(jsonPath("$.userEmail", is("test.user@example.com")));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - Success, Soft Deletes Own Task")
    @WithMockUser("user.a@example.com")
    void whenDeleteOwnTask_thenReturns204AndTaskIsSoftDeleted() throws Exception {
        final var taskToDelete = taskRepository.save(
                Task.builder()
                        .title("Task to be deleted")
                        .description("...")
                        .status(TaskStatus.DONE)
                        .appUser(userA)
                        .build()
        );

        mockMvc.perform(delete("/api/tasks/{id}", taskToDelete.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.findById(taskToDelete.getId()).isPresent(),
                "Task should not be found by standard findById method.");

        final var deletedTask = taskRepository.findByIdEvenIfDeleted(taskToDelete.getId())
                .orElseThrow(() -> new AssertionError("Task should exist in the database"));

        assertTrue(deletedTask.isDeleted(), "Task's isDeleted flag should be true.");
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - Fails, Deletes Another User's Task")
    @WithMockUser("user.a@example.com")
    void whenDeleteAnotherUsersTask_thenReturns404NotFound() throws Exception {
        final var taskOfUserB = taskRepository.save(
                Task.builder()
                        .title("User B's Task")
                        .description("This task should be safe.")
                        .status(TaskStatus.IN_PROGRESS)
                        .appUser(userB)
                        .build()
        );

        mockMvc.perform(delete("/api/tasks/{id}", taskOfUserB.getId())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - Success, Updates Own Task")
    @WithMockUser("user.a@example.com")
    void whenUpdateOwnTask_thenReturns200AndUpdatedTask() throws Exception {
        final var originalTask = taskRepository.save(
                Task.builder()
                        .title("Original Title")
                        .description("Original description.")
                        .status(TaskStatus.TO_DO)
                        .appUser(userA)
                        .build()
        );

        final var updateRequest = new TaskRequest(
                "Updated Title",
                "Updated description.",
                TaskStatus.IN_PROGRESS
        );

        mockMvc.perform(put("/api/tasks/{id}", originalTask.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.description", is("Updated description.")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - Fails, Updates Another User's Task")
    @WithMockUser("user.a@example.com")
    void whenUpdateAnotherUsersTask_thenReturns404NotFound() throws Exception {
        final var taskOfUserB = taskRepository.save(
                Task.builder()
                        .title("User B's Task")
                        .description("This task belongs to User B.")
                        .status(TaskStatus.TO_DO)
                        .appUser(userB)
                        .build()
        );

        final var updateRequest = new TaskRequest(
                "Attempted Update Title",
                "This update should fail.",
                TaskStatus.DONE
        );

        mockMvc.perform(put("/api/tasks/{id}", taskOfUserB.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tasks - Success, Retrieves Own Tasks")
    @WithMockUser("user.a@example.com")
    void whenGetAllTasks_thenReturnsListOfOwnTasks() throws Exception {
        taskRepository.save(
                Task.builder()
                        .title("Task A1")
                        .appUser(userA)
                        .status(TaskStatus.TO_DO)
                        .description("...")
                        .build()
        );

        taskRepository.save(
                Task.builder()
                        .title("Task A2")
                        .appUser(userA)
                        .status(TaskStatus.IN_PROGRESS)
                        .description("...")
                        .build()
        );

        taskRepository.save(
                Task.builder()
                        .title("Task B1")
                        .appUser(userB)
                        .status(TaskStatus.DONE)
                        .description("...")
                        .build()
        );

        mockMvc.perform(get("/api/tasks")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Success, Retrieves Own Task")
    @WithMockUser("user.a@example.com")
    void whenGetOwnTaskById_thenReturnsTaskResponse() throws Exception {
        final var taskA = taskRepository.save(
                Task.builder()
                        .title("My Specific Task")
                        .description("Details of my task.")
                        .status(TaskStatus.IN_PROGRESS)
                        .appUser(userA)
                        .build()
        );

        mockMvc.perform(get("/api/tasks/{id}", taskA.getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskA.getId().intValue())))
                .andExpect(jsonPath("$.title", is("My Specific Task")))
                .andExpect(jsonPath("$.userEmail", is("user.a@example.com")));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Fails, Retrieves Another User's Task")
    @WithMockUser("user.a@example.com")
    void whenGetAnotherUsersTaskById_thenReturns404NotFound() throws Exception {
        final var taskOfUserB = taskRepository.save(
                Task.builder()
                        .title("Secret Task")
                        .description("This belongs to User B.")
                        .status(TaskStatus.IN_PROGRESS)
                        .appUser(userB)
                        .build()
        );

        mockMvc.perform(get("/api/tasks/{id}", taskOfUserB.getId())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/tasks - Fails, Invalid Data (Blank Title)")
    @WithMockUser("user.a@example.com")
    void whenCreateTaskWithBlankTitle_thenReturns400BadRequest() throws Exception {
        final var invalidRequest = new CreateTaskRequest(
                "",
                "This description is fine."
        );

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", is("Title cannot be blank")));
    }

    @Test
    @DisplayName("GET /api/tasks - With Pagination")
    @WithMockUser("user@example.com")
    void whenGetTasksWithPagination_thenReturnsPagedResult() throws Exception {
        final var user = appUserRepository.save(
                AppUser.builder()
                        .email("user@example.com")
                        .name("Test User")
                        .password(passwordEncoder.encode("password"))
                        .build()
        );

        for (int i = 0; i < 15; i++) {
            taskRepository.save(Task.builder().title("Task " + i).description("...").status(TaskStatus.TO_DO).appUser(user).build());
        }

        mockMvc.perform(get("/api/tasks")
                        .param("page", "1")
                        .param("size", "5")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.totalElements", is(15)))
                .andExpect(jsonPath("$.number", is(1)));
    }

    @Test
    @DisplayName("GET /api/tasks - With Status Filter")
    @WithMockUser("filter.user@example.com")
    void whenGetTasksWithStatusFilter_thenReturnsFilteredTasks() throws Exception {
        final var filterUser = createAndSaveTestUser();
        createSampleTasksForUser(filterUser);

        mockMvc.perform(get("/api/tasks")
                        .param("status", TaskStatus.TO_DO.name())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].status", everyItem(is(TaskStatus.TO_DO.name()))));
    }

    @Test
    @DisplayName("GET /api/tasks - Without Filter Returns All Tasks")
    @WithMockUser("filter.user@example.com")
    void whenGetTasksWithoutFilter_thenReturnsAllTasks() throws Exception {
        final var filterUser = createAndSaveTestUser();
        createSampleTasksForUser(filterUser);

        mockMvc.perform(get("/api/tasks")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    private AppUser createAndSaveTestUser() {
        return appUserRepository.save(
                AppUser.builder()
                        .email("filter.user@example.com")
                        .name("Test User")
                        .password(passwordEncoder.encode("password"))
                        .build()
        );
    }

    private void createSampleTasksForUser(AppUser user) {
        taskRepository.save(Task.builder()
                .title("Task A")
                .description("Description for Task A")
                .status(TaskStatus.TO_DO)
                .appUser(user)
                .build());

        taskRepository.save(Task.builder()
                .title("Task B")
                .description("Description for Task B")
                .status(TaskStatus.DONE)
                .appUser(user)
                .build());

        taskRepository.save(Task.builder()
                .title("Task C")
                .description("Description for Task C")
                .status(TaskStatus.TO_DO)
                .appUser(user)
                .build());
    }
}