package com.chatgemma.service;

import com.chatgemma.entity.AuditLog;
import com.chatgemma.entity.Chat;
import com.chatgemma.entity.Message;
import com.chatgemma.repository.AuditLogRepository;
import com.chatgemma.repository.ChatRepository;
import com.chatgemma.repository.MessageRepository;
import com.chatgemma.service.exception.ChatNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final OllamaService ollamaService;

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository,
                      AuditLogRepository auditLogRepository, OllamaService ollamaService) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.auditLogRepository = auditLogRepository;
        this.ollamaService = ollamaService;
    }

    @Transactional
    public Chat createChat(Long userId, String title, String clientIp, String userAgent) {
        validateChatTitle(title);

        Chat chat = Chat.create(userId, title);
        Chat savedChat = chatRepository.save(chat);

        // 감사 로그 기록
        recordAuditLog(userId, "CREATE_CHAT", "CHAT", savedChat.getId(), clientIp, userAgent, null);

        return savedChat;
    }

    public List<Chat> getUserActiveChats(Long userId) {
        return chatRepository.findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(userId);
    }

    public Page<Chat> getUserActiveChats(Long userId, Pageable pageable) {
        return chatRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    public Chat getChatByIdAndUserId(Long chatId, Long userId) {
        return chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId)
                .orElseThrow(() -> new ChatNotFoundException("채팅을 찾을 수 없습니다"));
    }

    @Transactional
    public Chat updateChatTitle(Long chatId, Long userId, String newTitle, String clientIp, String userAgent) {
        validateChatTitle(newTitle);

        Chat chat = getChatByIdAndUserId(chatId, userId);
        String oldTitle = chat.getTitle(); // 수정 전 제목 저장
        chat.updateTitle(newTitle);
        Chat updatedChat = chatRepository.save(chat);

        // 감사 로그 기록
        recordAuditLog(userId, "UPDATE_CHAT", "CHAT", chatId, clientIp, userAgent,
                "{\"oldTitle\":\"" + oldTitle + "\",\"newTitle\":\"" + newTitle + "\"}");

        return updatedChat;
    }

    @Transactional
    public void deleteChat(Long chatId, Long userId, String clientIp, String userAgent) {
        Chat chat = getChatByIdAndUserId(chatId, userId);
        chat.softDelete();
        chatRepository.save(chat);

        // 감사 로그 기록
        recordAuditLog(userId, "DELETE_CHAT", "CHAT", chatId, clientIp, userAgent, null);
    }

    @Transactional
    public Message sendMessage(Long chatId, Long userId, String content, String imageUrl,
                              String clientIp, String userAgent) {
        // 입력 검증을 먼저 수행
        validateMessageContent(content);

        // 채팅 존재 및 권한 확인
        Chat chat = getChatByIdAndUserId(chatId, userId);

        // 사용자 메시지 저장
        Message userMessage;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            userMessage = Message.createUserMessageWithImage(chatId, content, imageUrl);
        } else {
            userMessage = Message.createUserMessage(chatId, content);
        }
        Message savedUserMessage = messageRepository.save(userMessage);

        try {
            // AI 응답 요청
            String aiResponse = ollamaService.sendMessage(content, imageUrl);

            // AI 응답 메시지 저장
            Message aiMessage = Message.createAssistantMessage(chatId, aiResponse);
            messageRepository.save(aiMessage);

            // 감사 로그 기록
            recordAuditLog(userId, "SEND_MESSAGE", "MESSAGE", savedUserMessage.getId(),
                    clientIp, userAgent, imageUrl != null ? "{\"hasImage\":true}" : null);

            return savedUserMessage;

        } catch (Exception e) {
            // AI 서비스 오류 시에도 사용자 메시지는 저장되어 있음
            recordAuditLog(userId, "AI_ERROR", "MESSAGE", savedUserMessage.getId(),
                    clientIp, userAgent, "{\"error\":\"" + e.getMessage() + "\"}");
            throw e;
        }
    }

    public List<Message> getChatMessages(Long chatId, Long userId) {
        // 채팅 존재 및 권한 확인
        getChatByIdAndUserId(chatId, userId);
        return messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    public Page<Message> getChatMessages(Long chatId, Long userId, Pageable pageable) {
        // 채팅 존재 및 권한 확인
        getChatByIdAndUserId(chatId, userId);
        return messageRepository.findByChatId(chatId, pageable);
    }

    public List<Chat> searchChats(Long userId, String keyword) {
        return chatRepository.findByUserIdAndTitleContainingAndDeletedFalse(userId, keyword);
    }

    public long getUserActiveChatCount(Long userId) {
        return chatRepository.countByUserIdAndDeletedFalse(userId);
    }

    private void validateChatTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅 제목은 필수입니다");
        }
    }

    private void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
    }

    // WebSocket용 스트리밍 메시지 처리
    @Transactional
    public void processMessageStreamAsync(com.chatgemma.dto.request.ChatMessageRequest request,
                                        String sessionId,
                                        java.util.function.Consumer<String> chunkConsumer) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Mock streaming response - 실제로는 Ollama AI 모델과 연동
                String[] words = {"안녕하세요!", " ", "무엇을", " ", "도와드릴까요?", " ",
                                 "ChatGemma는", " ", "실시간으로", " ", "응답을", " ", "제공합니다."};

                for (String word : words) {
                    chunkConsumer.accept(word);
                    try {
                        Thread.sleep(200); // 200ms 간격으로 단어별 전송
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // TODO: 실제 AI 응답을 DB에 저장하는 로직 추가

            } catch (Exception e) {
                chunkConsumer.accept("오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    private void recordAuditLog(Long userId, String action, String resourceType, Long resourceId,
                               String ipAddress, String userAgent, String details) {
        AuditLog auditLog;
        if (details != null) {
            auditLog = AuditLog.createWithDetails(userId, action, resourceType, resourceId,
                    ipAddress, userAgent, details);
        } else {
            auditLog = AuditLog.create(userId, action, resourceType, resourceId, ipAddress, userAgent);
        }
        auditLogRepository.save(auditLog);
    }
}