package com.chatgemma.dto.response;

import com.chatgemma.entity.Chat;

import java.time.LocalDateTime;

public class ChatResponse {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChatResponse() {}

    public ChatResponse(Chat chat) {
        this.id = chat.getId();
        this.title = chat.getTitle();
        this.createdAt = chat.getCreatedAt();
        this.updatedAt = chat.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}