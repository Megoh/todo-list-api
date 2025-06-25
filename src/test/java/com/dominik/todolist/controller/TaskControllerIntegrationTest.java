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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;


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
    @DisplayName("DELETE /api/tasks/{id} - Success, Deletes Own Task")
    @WithMockUser("user.a@example.com")
    void whenDeleteOwnTask_thenReturns204NoContent() throws Exception {
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
                "Task should have been deleted from the database");
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
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Task A1")))
                .andExpect(jsonPath("$[1].title", is("Task A2")));
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
        CreateTaskRequest invalidRequest = new CreateTaskRequest(
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
}