package com.chatgemma.repository;

import com.chatgemma.entity.Chat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("Chat Repository Tests")
class ChatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRepository chatRepository;

    private Chat user1ActiveChat;
    private Chat user1DeletedChat;
    private Chat user2ActiveChat;
    private Long user1Id = 1L;
    private Long user2Id = 2L;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        user1ActiveChat = Chat.create(user1Id, "User1의 활성 채팅");
        user1DeletedChat = Chat.create(user1Id, "User1의 삭제된 채팅");
        user1DeletedChat.softDelete();
        user2ActiveChat = Chat.create(user2Id, "User2의 활성 채팅");

        entityManager.persist(user1ActiveChat);
        entityManager.persist(user1DeletedChat);
        entityManager.persist(user2ActiveChat);
        entityManager.flush();
    }

    @Test
    @DisplayName("사용자별 활성 채팅 목록을 최신순으로 조회할 수 있다")
    void findByUserIdAndDeletedFalseOrderByUpdatedAtDesc_ShouldReturnActiveChatsInOrder() {
        // Given: 추가 채팅 생성
        Chat anotherActiveChat = Chat.create(user1Id, "또 다른 활성 채팅");
        entityManager.persist(anotherActiveChat);
        entityManager.flush();

        // When
        List<Chat> activeChats = chatRepository.findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(user1Id);

        // Then
        assertThat(activeChats).hasSize(2);
        assertThat(activeChats).extracting(Chat::getTitle)
                .containsExactly("또 다른 활성 채팅", "User1의 활성 채팅"); // updatedAt 최신순
        assertThat(activeChats).allMatch(Chat::isActive);
    }

    @Test
    @DisplayName("사용자별 활성 채팅을 페이지별로 조회할 수 있다")
    void findByUserIdAndDeletedFalse_ShouldReturnPagedActiveChats() {
        // Given: 여러 채팅 생성
        for (int i = 1; i <= 5; i++) {
            Chat chat = Chat.create(user1Id, "채팅 " + i);
            entityManager.persist(chat);
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 3);

        // When
        Page<Chat> chatPage = chatRepository.findByUserIdAndDeletedFalse(user1Id, pageable);

        // Then
        assertThat(chatPage.getContent()).hasSize(3);
        assertThat(chatPage.getTotalElements()).isEqualTo(6); // 기존 1개 + 추가 5개
        assertThat(chatPage.getTotalPages()).isEqualTo(2);
        assertThat(chatPage.getContent()).allMatch(Chat::isActive);
    }

    @Test
    @DisplayName("사용자별 모든 채팅(삭제된 것 포함)을 조회할 수 있다")
    void findByUserId_ShouldReturnAllChatsIncludingDeleted() {
        // When
        List<Chat> allChats = chatRepository.findByUserId(user1Id);

        // Then
        assertThat(allChats).hasSize(2);
        assertThat(allChats).extracting(Chat::getTitle)
                .containsExactlyInAnyOrder("User1의 활성 채팅", "User1의 삭제된 채팅");
        assertThat(allChats).anyMatch(Chat::isActive);
        assertThat(allChats).anyMatch(chat -> !chat.isActive());
    }

    @Test
    @DisplayName("채팅 ID와 사용자 ID로 활성 채팅을 찾을 수 있다")
    void findByIdAndUserIdAndDeletedFalse_ShouldReturnActiveChat_WhenExists() {
        // When
        Optional<Chat> found = chatRepository.findByIdAndUserIdAndDeletedFalse(
                user1ActiveChat.getId(), user1Id);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("User1의 활성 채팅");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("삭제된 채팅을 ID와 사용자 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdAndUserIdAndDeletedFalse_ShouldReturnEmpty_WhenChatIsDeleted() {
        // When
        Optional<Chat> found = chatRepository.findByIdAndUserIdAndDeletedFalse(
                user1DeletedChat.getId(), user1Id);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 채팅을 조회하면 빈 Optional을 반환한다")
    void findByIdAndUserIdAndDeletedFalse_ShouldReturnEmpty_WhenUserIdNotMatch() {
        // When
        Optional<Chat> found = chatRepository.findByIdAndUserIdAndDeletedFalse(
                user1ActiveChat.getId(), user2Id);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자별 활성 채팅 수를 조회할 수 있다")
    void countByUserIdAndDeletedFalse_ShouldReturnCorrectCount() {
        // When
        long user1ActiveCount = chatRepository.countByUserIdAndDeletedFalse(user1Id);
        long user2ActiveCount = chatRepository.countByUserIdAndDeletedFalse(user2Id);

        // Then
        assertThat(user1ActiveCount).isEqualTo(1);
        assertThat(user2ActiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자별 전체 채팅 수를 조회할 수 있다")
    void countByUserId_ShouldReturnTotalChatCount() {
        // When
        long user1TotalCount = chatRepository.countByUserId(user1Id);
        long user2TotalCount = chatRepository.countByUserId(user2Id);

        // Then
        assertThat(user1TotalCount).isEqualTo(2); // 활성 1개 + 삭제된 1개
        assertThat(user2TotalCount).isEqualTo(1);
    }

    @Test
    @DisplayName("제목으로 활성 채팅을 검색할 수 있다")
    void findByUserIdAndTitleContainingAndDeletedFalse_ShouldReturnMatchingChats() {
        // Given: 검색을 위한 추가 채팅 생성
        Chat searchableChat = Chat.create(user1Id, "검색 가능한 특별한 채팅");
        entityManager.persist(searchableChat);
        entityManager.flush();

        // When
        List<Chat> foundChats = chatRepository.findByUserIdAndTitleContainingAndDeletedFalse(user1Id, "특별한");

        // Then
        assertThat(foundChats).hasSize(1);
        assertThat(foundChats.get(0).getTitle()).contains("특별한");
        assertThat(foundChats.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 키워드로 검색하면 빈 목록을 반환한다")
    void findByUserIdAndTitleContainingAndDeletedFalse_ShouldReturnEmpty_WhenNoMatch() {
        // When
        List<Chat> foundChats = chatRepository.findByUserIdAndTitleContainingAndDeletedFalse(user1Id, "존재하지않는키워드");

        // Then
        assertThat(foundChats).isEmpty();
    }

    @Test
    @DisplayName("최근 활성 채팅 목록을 조회할 수 있다")
    void findRecentActiveChats_ShouldReturnRecentChats() {
        // Given: 여러 사용자의 채팅 생성
        Chat user3Chat = Chat.create(3L, "User3의 채팅");
        Chat user4Chat = Chat.create(4L, "User4의 채팅");
        entityManager.persist(user3Chat);
        entityManager.persist(user4Chat);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 5);

        // When
        List<Chat> recentChats = chatRepository.findRecentActiveChats(pageable);

        // Then
        assertThat(recentChats).hasSize(4); // user1ActiveChat, user2ActiveChat, user3Chat, user4Chat
        assertThat(recentChats).allMatch(Chat::isActive);
        // updatedAt으로 정렬되어야 함 (최신이 먼저)
    }

    @Test
    @DisplayName("사용자가 특정 채팅에 대한 접근 권한이 있는지 확인할 수 있다")
    void existsByIdAndUserId_ShouldReturnCorrectResult() {
        // When & Then
        assertThat(chatRepository.existsByIdAndUserId(user1ActiveChat.getId(), user1Id)).isTrue();
        assertThat(chatRepository.existsByIdAndUserId(user1DeletedChat.getId(), user1Id)).isTrue(); // 삭제된 것도 포함
        assertThat(chatRepository.existsByIdAndUserId(user1ActiveChat.getId(), user2Id)).isFalse();
        assertThat(chatRepository.existsByIdAndUserId(999L, user1Id)).isFalse(); // 존재하지 않는 채팅
    }
}