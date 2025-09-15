package com.chatgemma.dto.response;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private String id;
    private String chatId;
    private String content;
    private String role; // USER, ASSISTANT, SYSTEM
    private LocalDateTime timestamp;
    private String imageUrl;
    private boolean isStreaming = false;
    private boolean isError = false;

    public ChatMessageResponse() {}

    private ChatMessageResponse(Builder builder) {
        this.id = builder.id;
        this.chatId = builder.chatId;
        this.content = builder.content;
        this.role = builder.role;
        this.timestamp = builder.timestamp;
        this.imageUrl = builder.imageUrl;
        this.isStreaming = builder.isStreaming;
        this.isError = builder.isError;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String chatId;
        private String content;
        private String role;
        private LocalDateTime timestamp;
        private String imageUrl;
        private boolean isStreaming = false;
        private boolean isError = false;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder chatId(String chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder isStreaming(boolean isStreaming) {
            this.isStreaming = isStreaming;
            return this;
        }

        public Builder isError(boolean isError) {
            this.isError = isError;
            return this;
        }

        public ChatMessageResponse build() {
            return new ChatMessageResponse(this);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isStreaming() { return isStreaming; }
    public void setStreaming(boolean streaming) { isStreaming = streaming; }

    public boolean isError() { return isError; }
    public void setError(boolean error) { isError = error; }
}