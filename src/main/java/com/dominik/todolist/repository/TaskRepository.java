package com.dominik.todolist.repository;

import com.dominik.todolist.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAppUser_Id(Long appUserId);

    List<Task> findAllByAppUser_Email(String email);

    Optional<Task> findByIdAndAppUser_Id(Long taskId, Long appUserId);
}
