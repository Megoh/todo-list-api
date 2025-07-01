package com.dominik.todolist.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "tasks"})
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Column(nullable = false)
    private String password;

    @Builder.Default
    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    public AppUser(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setAppUser(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setAppUser(null);
    }
}
