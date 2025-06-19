package com.dominik.todolist.model;

public enum TaskStatus {
    TO_DO,
    IN_PROGRESS,
    DONE;

    @Override
    public String toString() {
        return this.name().toLowerCase().replace("_", " ");
    }
}