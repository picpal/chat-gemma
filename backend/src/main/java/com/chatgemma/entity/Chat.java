package com.chatgemma.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chats", indexes = {
        @Index(name = "idx_chat_user_id", columnList = "userId"),
        @Index(name = "idx_chat_created_at", columnList = "createdAt"),
        @Index(name = "idx_chat_deleted", columnList = "deleted")
})
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    protected Chat() {
        // JPA를 위한 기본 생성자
    }

    private Chat(Long userId, String title) {
        validateRequired(userId, "사용자 ID는 필수입니다");
        validateTitle(title);

        this.userId = userId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.deleted = false;
    }

    public static Chat create(Long userId, String title) {
        return new Chat(userId, title);
    }

    public void updateTitle(String newTitle) {
        if (this.deleted) {
            throw new IllegalStateException("삭제된 채팅은 수정할 수 없습니다");
        }
        validateTitle(newTitle);

        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        if (this.deleted) {
            throw new IllegalStateException("이미 삭제된 채팅입니다");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return !this.deleted;
    }

    private void validateRequired(Long value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅 제목은 필수입니다");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("채팅 제목은 100자를 초과할 수 없습니다");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(id, chat.id) && Objects.equals(userId, chat.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "Chat{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deleted=" + deleted +
                '}';
    }
}