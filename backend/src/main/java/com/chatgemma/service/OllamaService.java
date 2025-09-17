package com.chatgemma.service;

import com.chatgemma.entity.Message;
import java.util.List;

public interface OllamaService {
    String sendMessage(String message, String imageUrl);

    String sendMessageWithContext(String message, String imageUrl, List<Message> recentMessages);
}