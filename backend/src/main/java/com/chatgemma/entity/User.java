package com.chatgemma.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_status", columnList = "status")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private Long approvedBy;

    protected User() {
        // JPA를 위한 기본 생성자
    }

    private User(String username, String password, String email, Role role, Status status) {
        validateRequired(username, "사용자명은 필수입니다");
        validateRequired(password, "패스워드는 필수입니다");
        validateRequired(email, "이메일은 필수입니다");

        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = LocalDateTime.now();

        if (status == Status.APPROVED) {
            this.approvedAt = LocalDateTime.now();
        }
    }

    public static User createUser(String username, String password, String email) {
        return new User(username, password, email, Role.USER, Status.PENDING);
    }

    public static User createAdmin(String username, String password, String email) {
        return new User(username, password, email, Role.ADMIN, Status.APPROVED);
    }

    public void approve(Long adminId) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("이미 처리된 사용자입니다");
        }
        this.status = Status.APPROVED;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(Long adminId) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("이미 처리된 사용자입니다");
        }
        this.status = Status.REJECTED;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return this.status == Status.APPROVED;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    // Enums
    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}