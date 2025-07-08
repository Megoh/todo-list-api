package com.dominik.todolist.service;

import com.dominik.todolist.config.TaskCleanupProperties;
import com.dominik.todolist.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class TaskCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCleanupService.class);

    private final TaskRepository taskRepository;
    private final TaskCleanupProperties cleanupProperties;

    public TaskCleanupService(TaskRepository taskRepository, TaskCleanupProperties cleanupProperties) {
        this.taskRepository = taskRepository;
        this.cleanupProperties = cleanupProperties;
        LOGGER.info("TaskCleanupService initialized with a retention period of {} days.",
                cleanupProperties.retentionPeriodDays());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void purgeDeletedTasks() {
        LOGGER.info("Starting scheduled task: Purging old soft-deleted tasks.");

        final Instant cutoffDate = Instant.now().minus(
                cleanupProperties.retentionPeriodDays(),
                ChronoUnit.DAYS);
        LOGGER.debug("Calculated cutoff date for task purge: {}", cutoffDate);

        final int purgedTaskCount = taskRepository.deleteTasksMarkedForDeletionBefore(cutoffDate);

        if (purgedTaskCount > 0) {
            LOGGER.info("Successfully purged {} old soft-deleted tasks.", purgedTaskCount);
        } else {
            LOGGER.info("No old soft-deleted tasks found to purge.");
        }
    }
}