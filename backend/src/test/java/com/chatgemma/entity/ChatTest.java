package com.chatgemma.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Chat Entity Tests")
class ChatTest {

    @Test
    @DisplayName("채팅 생성 시 기본값이 올바르게 설정된다")
    void createChat_ShouldSetDefaultValues() {
        // Given
        Long userId = 1L;
        String title = "새로운 채팅";

        // When
        Chat chat = Chat.create(userId, title);

        // Then
        assertThat(chat.getUserId()).isEqualTo(userId);
        assertThat(chat.getTitle()).isEqualTo(title);
        assertThat(chat.getCreatedAt()).isNotNull();
        assertThat(chat.getUpdatedAt()).isNotNull();
        assertThat(chat.isDeleted()).isFalse();
        assertThat(chat.getCreatedAt()).isEqualTo(chat.getUpdatedAt());
    }

    @Test
    @DisplayName("채팅 제목 변경 시 updatedAt이 갱신된다")
    void updateTitle_ShouldUpdateTimestamp() {
        // Given
        Chat chat = Chat.create(1L, "기존 제목");
        LocalDateTime originalUpdatedAt = chat.getUpdatedAt();
        String newTitle = "새로운 제목";

        // When
        chat.updateTitle(newTitle);

        // Then
        assertThat(chat.getTitle()).isEqualTo(newTitle);
        assertThat(chat.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("채팅 삭제 시 deleted 플래그가 true로 설정되고 updatedAt이 갱신된다")
    void softDelete_ShouldMarkAsDeletedAndUpdateTimestamp() {
        // Given
        Chat chat = Chat.create(1L, "테스트 채팅");
        LocalDateTime originalUpdatedAt = chat.getUpdatedAt();

        // When
        chat.softDelete();

        // Then
        assertThat(chat.isDeleted()).isTrue();
        assertThat(chat.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("이미 삭제된 채팅을 다시 삭제하려고 하면 예외가 발생한다")
    void softDelete_AlreadyDeletedChat_ShouldThrowException() {
        // Given
        Chat chat = Chat.create(1L, "테스트 채팅");
        chat.softDelete();

        // When & Then
        assertThatThrownBy(() -> chat.softDelete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 삭제된 채팅입니다");
    }

    @Test
    @DisplayName("삭제된 채팅의 제목을 변경하려고 하면 예외가 발생한다")
    void updateTitle_DeletedChat_ShouldThrowException() {
        // Given
        Chat chat = Chat.create(1L, "테스트 채팅");
        chat.softDelete();

        // When & Then
        assertThatThrownBy(() -> chat.updateTitle("새로운 제목"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제된 채팅은 수정할 수 없습니다");
    }

    @Test
    @DisplayName("사용자 ID가 null이면 예외가 발생한다")
    void create_WithNullUserId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Chat.create(null, "제목"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");
    }

    @Test
    @DisplayName("제목이 null이면 예외가 발생한다")
    void create_WithNullTitle_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Chat.create(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 제목은 필수입니다");
    }

    @Test
    @DisplayName("제목이 빈 문자열이면 예외가 발생한다")
    void create_WithEmptyTitle_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Chat.create(1L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 제목은 필수입니다");
    }

    @Test
    @DisplayName("새로운 제목이 null이면 예외가 발생한다")
    void updateTitle_WithNullTitle_ShouldThrowException() {
        // Given
        Chat chat = Chat.create(1L, "기존 제목");

        // When & Then
        assertThatThrownBy(() -> chat.updateTitle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 제목은 필수입니다");
    }

    @Test
    @DisplayName("제목 길이가 100자를 초과하면 예외가 발생한다")
    void create_WithTitleTooLong_ShouldThrowException() {
        // Given
        String longTitle = "a".repeat(101);

        // When & Then
        assertThatThrownBy(() -> Chat.create(1L, longTitle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 제목은 100자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("채팅이 활성상태인지 확인한다")
    void isActive_ShouldReturnCorrectStatus() {
        // Given
        Chat activeChat = Chat.create(1L, "활성 채팅");
        Chat deletedChat = Chat.create(2L, "삭제될 채팅");
        deletedChat.softDelete();

        // When & Then
        assertThat(activeChat.isActive()).isTrue();
        assertThat(deletedChat.isActive()).isFalse();
    }
}