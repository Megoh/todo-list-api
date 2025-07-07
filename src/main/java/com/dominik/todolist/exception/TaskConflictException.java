package com.dominik.todolist.exception;

public class TaskConflictException extends RuntimeException {
    public TaskConflictException(String message) {
        super(message);
    }
}