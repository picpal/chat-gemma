package com.chatgemma.dto.response;

import com.chatgemma.entity.Message;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private String role;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;

    public MessageResponse() {}

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.role = message.getRole().name();
        this.content = message.getContent();
        this.imageUrl = message.getImageUrl();
        this.createdAt = message.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}