package com.dominik.todolist.repository;

import com.dominik.todolist.model.Task;
import com.dominik.todolist.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE t.appUser.id = :userId AND (:status IS NULL OR t.status = :status)")
    Page<Task> findByUserIdAndOptionalStatus(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            Pageable pageable);
}
