package com.dominik.todolist.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.task.cleanup")
@Validated
public record TaskCleanupProperties(
        @Min(1)
        @DefaultValue("30")
        int retentionPeriodDays
) {

}
