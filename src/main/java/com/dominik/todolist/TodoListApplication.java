package com.dominik.todolist;

import com.dominik.todolist.config.TaskCleanupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TaskCleanupProperties.class)
public class TodoListApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoListApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TodoListApplication.class, args);
        LOGGER.info("Todo List API application has started.");
    }
}