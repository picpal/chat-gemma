package com.chatgemma.service;

import com.chatgemma.entity.Chat;
import com.chatgemma.entity.Message;
import com.chatgemma.entity.Message.Role;
import com.chatgemma.repository.AuditLogRepository;
import com.chatgemma.repository.ChatRepository;
import com.chatgemma.repository.MessageRepository;
import com.chatgemma.service.exception.ChatNotFoundException;
import com.chatgemma.service.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private OllamaService ollamaService;

    @InjectMocks
    private ChatService chatService;

    private Long userId = 1L;
    private Long chatId = 100L;
    private Chat activeChat;
    private String clientIp = "192.168.1.1";
    private String userAgent = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        activeChat = Chat.create(userId, "테스트 채팅");
        setChatId(activeChat, chatId);
    }

    private void setChatId(Chat chat, Long id) {
        try {
            var field = Chat.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(chat, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("새 채팅을 생성할 수 있다")
    void createChat_ShouldCreateNewChat_WhenValidInput() {
        // Given
        String title = "새로운 채팅";
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Chat result = chatService.createChat(userId, title, clientIp, userAgent);

        // Then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.isActive()).isTrue();

        verify(chatRepository).save(any(Chat.class));
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("CREATE_CHAT") &&
            log.getUserId().equals(userId) &&
            log.getResourceType().equals("CHAT")
        ));
    }

    @Test
    @DisplayName("사용자의 활성 채팅 목록을 조회할 수 있다")
    void getUserActiveChats_ShouldReturnActiveChats() {
        // Given
        List<Chat> activeChats = List.of(activeChat);
        when(chatRepository.findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(userId))
                .thenReturn(activeChats);

        // When
        List<Chat> result = chatService.getUserActiveChats(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(activeChat);
        verify(chatRepository).findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(userId);
    }

    @Test
    @DisplayName("사용자의 활성 채팅을 페이지별로 조회할 수 있다")
    void getUserActiveChats_ShouldReturnPagedChats() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Chat> chatPage = new PageImpl<>(List.of(activeChat));
        when(chatRepository.findByUserIdAndDeletedFalse(userId, pageable))
                .thenReturn(chatPage);

        // When
        Page<Chat> result = chatService.getUserActiveChats(userId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(activeChat);
        verify(chatRepository).findByUserIdAndDeletedFalse(userId, pageable);
    }

    @Test
    @DisplayName("채팅 ID와 사용자 ID로 채팅을 조회할 수 있다")
    void getChatByIdAndUserId_ShouldReturnChat_WhenExists() {
        // Given
        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));

        // When
        Chat result = chatService.getChatByIdAndUserId(chatId, userId);

        // Then
        assertThat(result).isEqualTo(activeChat);
        verify(chatRepository).findByIdAndUserIdAndDeletedFalse(chatId, userId);
    }

    @Test
    @DisplayName("존재하지 않는 채팅을 조회하면 예외가 발생한다")
    void getChatByIdAndUserId_ShouldThrowException_WhenNotExists() {
        // Given
        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.getChatByIdAndUserId(chatId, userId))
                .isInstanceOf(ChatNotFoundException.class)
                .hasMessage("채팅을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("채팅 제목을 업데이트할 수 있다")
    void updateChatTitle_ShouldUpdateTitle_WhenValid() {
        // Given
        String newTitle = "업데이트된 제목";
        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Chat result = chatService.updateChatTitle(chatId, userId, newTitle, clientIp, userAgent);

        // Then
        assertThat(result.getTitle()).isEqualTo(newTitle);
        verify(chatRepository).save(activeChat);
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("UPDATE_CHAT") &&
            log.getResourceId().equals(chatId)
        ));
    }

    @Test
    @DisplayName("채팅을 소프트 삭제할 수 있다")
    void deleteChat_ShouldSoftDeleteChat() {
        // Given
        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        chatService.deleteChat(chatId, userId, clientIp, userAgent);

        // Then
        assertThat(activeChat.isDeleted()).isTrue();
        verify(chatRepository).save(activeChat);
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("DELETE_CHAT") &&
            log.getResourceId().equals(chatId)
        ));
    }

    @Test
    @DisplayName("채팅에 사용자 메시지를 추가하고 AI 응답을 받을 수 있다")
    void sendMessage_ShouldAddMessageAndGetAIResponse_WhenTextMessage() {
        // Given
        String userMessageContent = "안녕하세요";
        String aiResponse = "안녕하세요! 무엇을 도와드릴까요?";

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ollamaService.sendMessage(eq(userMessageContent), isNull()))
                .thenReturn(aiResponse);

        // When
        Message result = chatService.sendMessage(chatId, userId, userMessageContent, null, clientIp, userAgent);

        // Then
        assertThat(result.getChatId()).isEqualTo(chatId);
        assertThat(result.getContent()).isEqualTo(userMessageContent);
        assertThat(result.getRole()).isEqualTo(Role.USER);

        verify(messageRepository, times(2)).save(any(Message.class)); // 사용자 메시지 + AI 응답
        verify(ollamaService).sendMessage(userMessageContent, null);
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("SEND_MESSAGE")
        ));
    }

    @Test
    @DisplayName("이미지가 포함된 메시지를 전송할 수 있다")
    void sendMessage_ShouldHandleImageMessage_WhenImageProvided() {
        // Given
        String userMessageContent = "이 이미지에 대해 설명해주세요";
        String imageUrl = "http://example.com/image.jpg";
        String aiResponse = "이미지를 분석한 결과입니다.";

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ollamaService.sendMessage(eq(userMessageContent), eq(imageUrl)))
                .thenReturn(aiResponse);

        // When
        Message result = chatService.sendMessage(chatId, userId, userMessageContent, imageUrl, clientIp, userAgent);

        // Then
        assertThat(result.hasImage()).isTrue();
        assertThat(result.getImageUrl()).isEqualTo(imageUrl);
        verify(ollamaService).sendMessage(userMessageContent, imageUrl);
    }

    @Test
    @DisplayName("채팅의 메시지 목록을 조회할 수 있다")
    void getChatMessages_ShouldReturnMessages() {
        // Given
        Message userMessage = Message.createUserMessage(chatId, "사용자 메시지");
        Message aiMessage = Message.createAssistantMessage(chatId, "AI 응답");
        List<Message> messages = List.of(userMessage, aiMessage);

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(messageRepository.findByChatIdOrderByCreatedAtAsc(chatId))
                .thenReturn(messages);

        // When
        List<Message> result = chatService.getChatMessages(chatId, userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(userMessage, aiMessage);
    }

    @Test
    @DisplayName("채팅의 메시지를 페이지별로 조회할 수 있다")
    void getChatMessages_ShouldReturnPagedMessages() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Message userMessage = Message.createUserMessage(chatId, "사용자 메시지");
        Page<Message> messagePage = new PageImpl<>(List.of(userMessage));

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(messageRepository.findByChatId(chatId, pageable))
                .thenReturn(messagePage);

        // When
        Page<Message> result = chatService.getChatMessages(chatId, userId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(userMessage);
    }

    @Test
    @DisplayName("제목으로 채팅을 검색할 수 있다")
    void searchChats_ShouldReturnMatchingChats() {
        // Given
        String keyword = "테스트";
        List<Chat> searchResults = List.of(activeChat);

        when(chatRepository.findByUserIdAndTitleContainingAndDeletedFalse(userId, keyword))
                .thenReturn(searchResults);

        // When
        List<Chat> result = chatService.searchChats(userId, keyword);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(activeChat);
        verify(chatRepository).findByUserIdAndTitleContainingAndDeletedFalse(userId, keyword);
    }

    @Test
    @DisplayName("사용자별 활성 채팅 수를 조회할 수 있다")
    void getUserActiveChatCount_ShouldReturnCorrectCount() {
        // Given
        long expectedCount = 5;
        when(chatRepository.countByUserIdAndDeletedFalse(userId)).thenReturn(expectedCount);

        // When
        long result = chatService.getUserActiveChatCount(userId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(chatRepository).countByUserIdAndDeletedFalse(userId);
    }

    @Test
    @DisplayName("입력 검증 - null 제목으로 채팅 생성 시 예외가 발생한다")
    void createChat_ShouldThrowException_WhenTitleIsNull() {
        // When & Then
        assertThatThrownBy(() -> chatService.createChat(userId, null, clientIp, userAgent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 제목은 필수입니다");
    }

    @Test
    @DisplayName("입력 검증 - 빈 메시지 내용으로 전송 시 예외가 발생한다")
    void sendMessage_ShouldThrowException_WhenContentIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(chatId, userId, "", null, clientIp, userAgent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메시지 내용은 필수입니다");
    }

    @Test
    @DisplayName("AI 서비스 오류 시 적절히 처리한다")
    void sendMessage_ShouldHandleAIServiceError() {
        // Given
        String userMessageContent = "안녕하세요";
        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(activeChat));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ollamaService.sendMessage(eq(userMessageContent), isNull()))
                .thenThrow(new RuntimeException("AI 서비스 오류"));

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(chatId, userId, userMessageContent, null, clientIp, userAgent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI 서비스 오류");

        // 사용자 메시지는 저장되어야 함
        verify(messageRepository, times(1)).save(any(Message.class));
    }
}