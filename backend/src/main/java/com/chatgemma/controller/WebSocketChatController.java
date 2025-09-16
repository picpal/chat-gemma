package com.chatgemma.controller;

import com.chatgemma.dto.request.ChatMessageRequest;
import com.chatgemma.dto.response.ChatMessageResponse;
import com.chatgemma.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003", "http://localhost:3004", "http://localhost:3005"})
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);

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
        String sessionId = headerAccessor.getSessionId();
        logger.info("📤 WebSocket message received: chatId={}, content={}, principal={}, sessionId={}",
                   request.getChatId(), request.getContent(), principal, sessionId);

        try {
            // 임시로 principal 없어도 처리하도록 수정
            if (principal == null) {
                logger.warn("⚠️ No authenticated user, proceeding with anonymous processing");
            }

            // 사용자 메시지는 프론트엔드에서 이미 표시하므로, 여기서는 AI 응답만 처리
            logger.info("🤖 Processing AI response for message: {}", request.getContent());

            // AI 응답을 스트리밍으로 전송 (비동기)
            String aiMessageId = System.currentTimeMillis() + "_ai";
            chatService.processMessageStreamAsync(request, sessionId, (chunk) -> {
                ChatMessageResponse aiChunk = ChatMessageResponse.builder()
                    .id(aiMessageId)
                    .chatId(request.getChatId())
                    .content(chunk)
                    .role("ASSISTANT")
                    .timestamp(LocalDateTime.now())
                    .isStreaming(true)
                    .build();

                // AI 응답 청크를 실시간으로 전송
                logger.info("🔄 Sending AI chunk to sessionId={}: {}", sessionId, chunk);

                // 채팅방 브로드캐스트 방식 사용
                String destination = "/topic/chat/" + request.getChatId();
                logger.info("📍 Broadcasting to destination: {}", destination);
                messagingTemplate.convertAndSend(destination, aiChunk);
            }).thenRun(() -> {
                // 스트리밍 완료 표시
                ChatMessageResponse completionMessage = ChatMessageResponse.builder()
                    .id(aiMessageId)
                    .chatId(request.getChatId())
                    .content("")
                    .role("ASSISTANT")
                    .timestamp(LocalDateTime.now())
                    .isStreaming(false)
                    .build();

                logger.info("✅ Sending streaming completion signal to sessionId={}", sessionId);

                // 완료 신호도 채팅방 브로드캐스트 방식 사용
                String destination = "/topic/chat/" + request.getChatId();
                logger.info("📍 Broadcasting completion to destination: {}", destination);
                messagingTemplate.convertAndSend(destination, completionMessage);
            });

        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);

            // 에러 메시지 전송
            ChatMessageResponse errorMessage = ChatMessageResponse.builder()
                .id(System.currentTimeMillis() + "_error")
                .chatId(request.getChatId())
                .content("죄송합니다. 오류가 발생했습니다: " + e.getMessage())
                .role("SYSTEM")
                .timestamp(LocalDateTime.now())
                .isError(true)
                .build();

            String destination = "/queue/chat/" + request.getChatId();
            logger.info("📍 Error message to destination: /user/{}/queue/chat/{}", headerAccessor.getSessionId(), request.getChatId());
            messagingTemplate.convertAndSendToUser(
                headerAccessor.getSessionId(),
                destination,
                errorMessage
            );
        }
    }

    @MessageMapping("/chat.join")
    public void joinChat(@Payload String chatId,
                        SimpMessageHeaderAccessor headerAccessor,
                        Principal principal) {
        logger.info("WebSocket chat join: chatId={}, principal={}, sessionId={}",
                   chatId, principal, headerAccessor.getSessionId());

        // 채팅방 참가 로직
        String sessionId = headerAccessor.getSessionId();

        // 채팅 히스토리 로드하여 전송
        try {
            // TODO: 실제 채팅 히스토리 로드 구현
            logger.info("User joined chat room: {}", chatId);

        } catch (Exception e) {
            logger.error("Error joining chat room: {}", chatId, e);
        }
    }
}