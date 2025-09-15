package com.chatgemma.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatMessageRequest {

    @NotNull(message = "채팅 ID는 필수입니다")
    private String chatId;

    @NotBlank(message = "메시지는 필수입니다")
    @Size(max = 5000, message = "메시지는 5000자를 초과할 수 없습니다")
    private String content;

    private String imageUrl;

    public ChatMessageRequest() {}

    public ChatMessageRequest(String chatId, String content, String imageUrl) {
        this.chatId = chatId;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
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
}