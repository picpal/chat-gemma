package com.chatgemma.controller;

import com.chatgemma.dto.request.ChatMessageRequest;
import com.chatgemma.dto.response.ChatMessageResponse;
import com.chatgemma.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002"})
public class WebSocketChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest request,
                           SimpMessageHeaderAccessor headerAccessor,
                           Principal principal) {
        try {
            // 사용자 메시지를 먼저 저장하고 브로드캐스트
            String userId = principal != null ? principal.getName() : "anonymous";
            String sessionId = headerAccessor.getSessionId();

            // 사용자 메시지 응답 생성
            ChatMessageResponse userMessage = ChatMessageResponse.builder()
                .id(System.currentTimeMillis() + "_user")
                .chatId(request.getChatId())
                .content(request.getContent())
                .role("USER")
                .timestamp(LocalDateTime.now())
                .imageUrl(request.getImageUrl())
                .build();

            // 클라이언트에게 사용자 메시지 전송
            messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/chat/" + request.getChatId(),
                userMessage
            );

            // AI 응답을 스트리밍으로 전송 (비동기)
            chatService.processMessageStreamAsync(request, sessionId, (chunk) -> {
                ChatMessageResponse aiChunk = ChatMessageResponse.builder()
                    .id(System.currentTimeMillis() + "_ai")
                    .chatId(request.getChatId())
                    .content(chunk)
                    .role("ASSISTANT")
                    .timestamp(LocalDateTime.now())
                    .isStreaming(true)
                    .build();

                // AI 응답 청크를 실시간으로 전송
                messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/chat/" + request.getChatId(),
                    aiChunk
                );
            });

        } catch (Exception e) {
            // 에러 메시지 전송
            ChatMessageResponse errorMessage = ChatMessageResponse.builder()
                .id(System.currentTimeMillis() + "_error")
                .chatId(request.getChatId())
                .content("죄송합니다. 오류가 발생했습니다: " + e.getMessage())
                .role("SYSTEM")
                .timestamp(LocalDateTime.now())
                .isError(true)
                .build();

            messagingTemplate.convertAndSendToUser(
                headerAccessor.getSessionId(),
                "/queue/chat/" + request.getChatId(),
                errorMessage
            );
        }
    }

    @MessageMapping("/chat.join")
    public void joinChat(@Payload String chatId,
                        SimpMessageHeaderAccessor headerAccessor,
                        Principal principal) {
        // 채팅방 참가 로직
        String sessionId = headerAccessor.getSessionId();

        // 채팅 히스토리 로드하여 전송
        try {
            // TODO: 실제 채팅 히스토리 로드 구현
            ChatMessageResponse joinMessage = ChatMessageResponse.builder()
                .id(System.currentTimeMillis() + "_system")
                .chatId(chatId)
                .content("채팅방에 참가했습니다.")
                .role("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();

            messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/chat/" + chatId,
                joinMessage
            );
        } catch (Exception e) {
            // 에러 처리
        }
    }
}