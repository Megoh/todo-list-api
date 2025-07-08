package com.dominik.todolist.repository;

import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE t.appUser.id = :userId AND (:status IS NULL OR t.status = :status)")
    Page<Task> findByUserIdAndOptionalStatus(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            Pageable pageable);

    @Query(value = "SELECT * FROM tasks WHERE id = :id", nativeQuery = true)
    Optional<Task> findByIdEvenIfDeleted(@Param("id") Long id);

    @Modifying
    @Query(
            value = "DELETE FROM tasks WHERE is_deleted = true AND deleted_at < :cutoffDate",
            nativeQuery = true
    )
    int deleteTasksMarkedForDeletionBefore(@Param("cutoffDate") Instant cutoffDate);
}
