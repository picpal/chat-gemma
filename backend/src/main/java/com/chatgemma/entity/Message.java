package com.chatgemma.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_chat_id", columnList = "chatId"),
        @Index(name = "idx_message_created_at", columnList = "createdAt"),
        @Index(name = "idx_message_role", columnList = "role")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private Boolean excludeFromContext;

    protected Message() {
        // JPA를 위한 기본 생성자
    }

    private Message(Long chatId, Role role, String content, String imageUrl) {
        validateRequired(chatId, "채팅 ID는 필수입니다");
        validateContent(content);
        if (imageUrl != null) {
            validateImageUrl(imageUrl);
        }

        this.chatId = chatId;
        this.role = role;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
        this.excludeFromContext = false;
    }

    public static Message createUserMessage(Long chatId, String content) {
        return new Message(chatId, Role.USER, content, null);
    }

    public static Message createAssistantMessage(Long chatId, String content) {
        return new Message(chatId, Role.ASSISTANT, content, null);
    }

    public static Message createUserMessageWithImage(Long chatId, String content, String imageUrl) {
        validateImageUrl(imageUrl);
        return new Message(chatId, Role.USER, content, imageUrl);
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    public boolean isUserMessage() {
        return role == Role.USER;
    }

    public boolean isAssistantMessage() {
        return role == Role.ASSISTANT;
    }

    public void excludeFromContext() {
        this.excludeFromContext = true;
    }

    public boolean isExcludedFromContext() {
        return this.excludeFromContext != null && this.excludeFromContext;
    }

    private static void validateRequired(Long value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
        if (content.length() > 10000) {
            throw new IllegalArgumentException("메시지 내용은 10000자를 초과할 수 없습니다");
        }
    }

    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("이미지 URL은 필수입니다");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Boolean getExcludeFromContext() {
        return excludeFromContext;
    }

    // Enums
    public enum Role {
        USER, ASSISTANT
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
               Objects.equals(chatId, message.chatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chatId);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", role=" + role +
                ", content='" + content.substring(0, Math.min(content.length(), 50)) +
                (content.length() > 50 ? "..." : "") + '\'' +
                ", hasImage=" + hasImage() +
                ", createdAt=" + createdAt +
                '}';
    }
}