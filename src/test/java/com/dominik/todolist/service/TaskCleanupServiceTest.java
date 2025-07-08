package com.dominik.todolist.service;

import com.dominik.todolist.model.AppUser;
import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import com.dominik.todolist.repository.AppUserRepository;
import com.dominik.todolist.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class TaskCleanupServiceTest {

    @Autowired
    private TaskCleanupService taskCleanupService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        appUserRepository.deleteAll();

        testUser = appUserRepository.save(
                AppUser.builder()
                        .email("cleanup.user@example.com")
                        .name("Cleanup User")
                        .password(passwordEncoder.encode("password"))
                        .build()
        );
    }

    @Test
    @DisplayName("purgeDeletedTasks should permanently delete tasks older than retention period and keep recent ones")
    void whenPurgeIsRun_thenOldTasksAreDeletedAndNewOnesRemain() {
        Task oldTask = Task.builder()
                .title("Old Task to be Purged")
                .description("...")
                .status(TaskStatus.DONE)
                .appUser(testUser)
                .build();

        Task recentTask = Task.builder()
                .title("Recent Task to be Kept")
                .description("...")
                .status(TaskStatus.DONE)
                .appUser(testUser)
                .build();

        Task activeTask = Task.builder()
                .title("Active Task")
                .description("...")
                .status(TaskStatus.IN_PROGRESS)
                .appUser(testUser)
                .build();

        taskRepository.saveAll(List.of(oldTask, recentTask, activeTask));

        oldTask.setDeleted(true);
        oldTask.setDeletedAt(Instant.now().minus(40, ChronoUnit.DAYS));

        recentTask.setDeleted(true);
        recentTask.setDeletedAt(Instant.now().minus(10, ChronoUnit.DAYS));

        taskRepository.saveAll(List.of(oldTask, recentTask));

        taskCleanupService.purgeDeletedTasks();

        assertFalse(taskRepository.findByIdEvenIfDeleted(oldTask.getId()).isPresent(),
                "The old task should have been permanently purged from the database.");

        assertTrue(taskRepository.findByIdEvenIfDeleted(recentTask.getId()).isPresent(),
                "The recent soft-deleted task should NOT have been purged.");

        assertTrue(taskRepository.findById(activeTask.getId()).isPresent(),
                "The active task should not be affected by the cleanup process.");
    }
}