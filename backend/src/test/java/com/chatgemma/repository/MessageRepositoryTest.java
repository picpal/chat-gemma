package com.chatgemma.repository;

import com.chatgemma.entity.Message;
import com.chatgemma.entity.Message.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("Message Repository Tests")
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private Long chat1Id = 1L;
    private Long chat2Id = 2L;
    private Message userMessage1;
    private Message assistantMessage1;
    private Message userMessageWithImage;
    private Message userMessage2;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        userMessage1 = Message.createUserMessage(chat1Id, "첫 번째 사용자 메시지");
        assistantMessage1 = Message.createAssistantMessage(chat1Id, "첫 번째 어시스턴트 응답");
        userMessageWithImage = Message.createUserMessageWithImage(chat1Id, "이미지 설명 요청", "http://example.com/image.jpg");
        userMessage2 = Message.createUserMessage(chat2Id, "다른 채팅의 메시지");

        entityManager.persist(userMessage1);
        entityManager.persist(assistantMessage1);
        entityManager.persist(userMessageWithImage);
        entityManager.persist(userMessage2);
        entityManager.flush();
    }

    @Test
    @DisplayName("채팅별 메시지 목록을 시간순으로 조회할 수 있다")
    void findByChatIdOrderByCreatedAtAsc_ShouldReturnMessagesInOrder() {
        // When
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chat1Id);

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isEqualTo(userMessage1);
        assertThat(messages.get(1)).isEqualTo(assistantMessage1);
        assertThat(messages.get(2)).isEqualTo(userMessageWithImage);
        // createdAt 순서대로 정렬되어 있어야 함
    }

    @Test
    @DisplayName("채팅별 메시지를 페이지별로 조회할 수 있다")
    void findByChatId_ShouldReturnPagedMessages() {
        // Given: 추가 메시지 생성
        for (int i = 1; i <= 5; i++) {
            Message message = Message.createUserMessage(chat1Id, "추가 메시지 " + i);
            entityManager.persist(message);
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 4);

        // When
        Page<Message> messagePage = messageRepository.findByChatId(chat1Id, pageable);

        // Then
        assertThat(messagePage.getContent()).hasSize(4);
        assertThat(messagePage.getTotalElements()).isEqualTo(8); // 기존 3개 + 추가 5개
        assertThat(messagePage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("채팅별 메시지 수를 조회할 수 있다")
    void countByChatId_ShouldReturnCorrectCount() {
        // When
        long chat1MessageCount = messageRepository.countByChatId(chat1Id);
        long chat2MessageCount = messageRepository.countByChatId(chat2Id);

        // Then
        assertThat(chat1MessageCount).isEqualTo(3);
        assertThat(chat2MessageCount).isEqualTo(1);
    }

    @Test
    @DisplayName("채팅의 최근 메시지를 조회할 수 있다")
    void findTopByChatIdOrderByCreatedAtDesc_ShouldReturnLatestMessage() {
        // When
        Message latestMessage = messageRepository.findTopByChatIdOrderByCreatedAtDesc(chat1Id);

        // Then
        assertThat(latestMessage).isEqualTo(userMessageWithImage); // 마지막에 생성된 메시지
    }

    @Test
    @DisplayName("메시지가 없는 채팅의 최근 메시지를 조회하면 null을 반환한다")
    void findTopByChatIdOrderByCreatedAtDesc_ShouldReturnNull_WhenNoChatExists() {
        // When
        Message latestMessage = messageRepository.findTopByChatIdOrderByCreatedAtDesc(999L);

        // Then
        assertThat(latestMessage).isNull();
    }

    @Test
    @DisplayName("채팅별 역할별 메시지 수를 조회할 수 있다")
    void countByChatIdAndRole_ShouldReturnCorrectCount() {
        // When
        long userMessageCount = messageRepository.countByChatIdAndRole(chat1Id, Role.USER);
        long assistantMessageCount = messageRepository.countByChatIdAndRole(chat1Id, Role.ASSISTANT);

        // Then
        assertThat(userMessageCount).isEqualTo(2); // userMessage1, userMessageWithImage
        assertThat(assistantMessageCount).isEqualTo(1); // assistantMessage1
    }

    @Test
    @DisplayName("이미지가 포함된 메시지를 조회할 수 있다")
    void findByChatIdAndImageUrlIsNotNull_ShouldReturnMessagesWithImages() {
        // When
        List<Message> messagesWithImages = messageRepository.findByChatIdAndImageUrlIsNotNull(chat1Id);

        // Then
        assertThat(messagesWithImages).hasSize(1);
        assertThat(messagesWithImages.get(0)).isEqualTo(userMessageWithImage);
        assertThat(messagesWithImages.get(0).hasImage()).isTrue();
    }

    @Test
    @DisplayName("채팅별 특정 시점 이후의 메시지를 조회할 수 있다")
    void findByChatIdAndCreatedAtAfter_ShouldReturnMessagesAfterTimestamp() {
        // Given
        LocalDateTime cutoffTime = assistantMessage1.getCreatedAt();

        // When
        List<Message> recentMessages = messageRepository.findByChatIdAndCreatedAtAfter(chat1Id, cutoffTime);

        // Then
        assertThat(recentMessages).hasSize(1);
        assertThat(recentMessages.get(0)).isEqualTo(userMessageWithImage);
    }

    @Test
    @DisplayName("메시지 내용으로 검색할 수 있다")
    void findByChatIdAndContentContaining_ShouldReturnMatchingMessages() {
        // When
        List<Message> foundMessages = messageRepository.findByChatIdAndContentContaining(chat1Id, "이미지");

        // Then
        assertThat(foundMessages).hasSize(1);
        assertThat(foundMessages.get(0)).isEqualTo(userMessageWithImage);
        assertThat(foundMessages.get(0).getContent()).contains("이미지");
    }

    @Test
    @DisplayName("존재하지 않는 키워드로 검색하면 빈 목록을 반환한다")
    void findByChatIdAndContentContaining_ShouldReturnEmpty_WhenNoMatch() {
        // When
        List<Message> foundMessages = messageRepository.findByChatIdAndContentContaining(chat1Id, "존재하지않는키워드");

        // Then
        assertThat(foundMessages).isEmpty();
    }

    @Test
    @DisplayName("채팅의 첫 번째 메시지를 조회할 수 있다")
    void findTopByChatIdOrderByCreatedAtAsc_ShouldReturnFirstMessage() {
        // When
        Message firstMessage = messageRepository.findTopByChatIdOrderByCreatedAtAsc(chat1Id);

        // Then
        assertThat(firstMessage).isEqualTo(userMessage1); // 처음에 생성된 메시지
    }

    @Test
    @DisplayName("여러 채팅의 메시지를 한 번에 삭제할 수 있다")
    void deleteByChatIdIn_ShouldDeleteMessagesOfMultipleChats() {
        // Given
        List<Long> chatIds = List.of(chat1Id, chat2Id);
        long initialTotalCount = messageRepository.count();

        // When
        messageRepository.deleteByChatIdIn(chatIds);
        entityManager.flush();
        entityManager.clear();

        // Then
        long finalTotalCount = messageRepository.count();
        assertThat(finalTotalCount).isEqualTo(0); // 모든 메시지가 삭제되어야 함
        assertThat(initialTotalCount).isEqualTo(4); // 삭제 전에는 4개였어야 함
    }

    @Test
    @DisplayName("사용자 메시지만 조회할 수 있다")
    void findByChatIdAndRole_ShouldReturnUserMessages() {
        // When
        List<Message> userMessages = messageRepository.findByChatIdAndRole(chat1Id, Role.USER);

        // Then
        assertThat(userMessages).hasSize(2);
        assertThat(userMessages).containsExactlyInAnyOrder(userMessage1, userMessageWithImage);
        assertThat(userMessages).allMatch(Message::isUserMessage);
    }

    @Test
    @DisplayName("어시스턴트 메시지만 조회할 수 있다")
    void findByChatIdAndRole_ShouldReturnAssistantMessages() {
        // When
        List<Message> assistantMessages = messageRepository.findByChatIdAndRole(chat1Id, Role.ASSISTANT);

        // Then
        assertThat(assistantMessages).hasSize(1);
        assertThat(assistantMessages).containsExactly(assistantMessage1);
        assertThat(assistantMessages).allMatch(Message::isAssistantMessage);
    }
}