package com.chatgemma.entity;

import com.chatgemma.entity.Message.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Message Entity Tests")
class MessageTest {

    @Test
    @DisplayName("사용자 메시지 생성 시 기본값이 올바르게 설정된다")
    void createUserMessage_ShouldSetDefaultValues() {
        // Given
        Long chatId = 1L;
        String content = "안녕하세요";

        // When
        Message message = Message.createUserMessage(chatId, content);

        // Then
        assertThat(message.getChatId()).isEqualTo(chatId);
        assertThat(message.getRole()).isEqualTo(Role.USER);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getImageUrl()).isNull();
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("어시스턴트 메시지 생성 시 기본값이 올바르게 설정된다")
    void createAssistantMessage_ShouldSetDefaultValues() {
        // Given
        Long chatId = 1L;
        String content = "안녕하세요! 무엇을 도와드릴까요?";

        // When
        Message message = Message.createAssistantMessage(chatId, content);

        // Then
        assertThat(message.getChatId()).isEqualTo(chatId);
        assertThat(message.getRole()).isEqualTo(Role.ASSISTANT);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getImageUrl()).isNull();
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미지가 포함된 사용자 메시지 생성 시 이미지 URL이 설정된다")
    void createUserMessageWithImage_ShouldSetImageUrl() {
        // Given
        Long chatId = 1L;
        String content = "이 이미지에 대해 설명해주세요";
        String imageUrl = "http://example.com/image.jpg";

        // When
        Message message = Message.createUserMessageWithImage(chatId, content, imageUrl);

        // Then
        assertThat(message.getChatId()).isEqualTo(chatId);
        assertThat(message.getRole()).isEqualTo(Role.USER);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getImageUrl()).isEqualTo(imageUrl);
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("채팅 ID가 null이면 예외가 발생한다")
    void createUserMessage_WithNullChatId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Message.createUserMessage(null, "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅 ID는 필수입니다");
    }

    @Test
    @DisplayName("내용이 null이면 예외가 발생한다")
    void createUserMessage_WithNullContent_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Message.createUserMessage(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메시지 내용은 필수입니다");
    }

    @Test
    @DisplayName("내용이 빈 문자열이면 예외가 발생한다")
    void createUserMessage_WithEmptyContent_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Message.createUserMessage(1L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메시지 내용은 필수입니다");
    }

    @Test
    @DisplayName("내용이 최대 길이를 초과하면 예외가 발생한다")
    void createUserMessage_WithContentTooLong_ShouldThrowException() {
        // Given
        String longContent = "a".repeat(10001);

        // When & Then
        assertThatThrownBy(() -> Message.createUserMessage(1L, longContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메시지 내용은 10000자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("이미지 URL이 null인 상태로 이미지 메시지를 생성하면 예외가 발생한다")
    void createUserMessageWithImage_WithNullImageUrl_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Message.createUserMessageWithImage(1L, "내용", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 URL은 필수입니다");
    }

    @Test
    @DisplayName("이미지 URL이 빈 문자열인 상태로 이미지 메시지를 생성하면 예외가 발생한다")
    void createUserMessageWithImage_WithEmptyImageUrl_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> Message.createUserMessageWithImage(1L, "내용", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 URL은 필수입니다");
    }

    @Test
    @DisplayName("이미지가 포함되어 있는지 확인한다")
    void hasImage_ShouldReturnCorrectStatus() {
        // Given
        Message textMessage = Message.createUserMessage(1L, "텍스트만");
        Message imageMessage = Message.createUserMessageWithImage(1L, "이미지 포함", "http://example.com/image.jpg");

        // When & Then
        assertThat(textMessage.hasImage()).isFalse();
        assertThat(imageMessage.hasImage()).isTrue();
    }

    @Test
    @DisplayName("사용자 메시지인지 확인한다")
    void isUserMessage_ShouldReturnCorrectStatus() {
        // Given
        Message userMessage = Message.createUserMessage(1L, "사용자 메시지");
        Message assistantMessage = Message.createAssistantMessage(1L, "어시스턴트 메시지");

        // When & Then
        assertThat(userMessage.isUserMessage()).isTrue();
        assertThat(assistantMessage.isUserMessage()).isFalse();
    }

    @Test
    @DisplayName("어시스턴트 메시지인지 확인한다")
    void isAssistantMessage_ShouldReturnCorrectStatus() {
        // Given
        Message userMessage = Message.createUserMessage(1L, "사용자 메시지");
        Message assistantMessage = Message.createAssistantMessage(1L, "어시스턴트 메시지");

        // When & Then
        assertThat(userMessage.isAssistantMessage()).isFalse();
        assertThat(assistantMessage.isAssistantMessage()).isTrue();
    }

    @Test
    @DisplayName("메시지 내용이 최대 길이 경계값을 정확히 처리한다")
    void createUserMessage_WithMaxLengthContent_ShouldSucceed() {
        // Given
        String maxLengthContent = "a".repeat(10000);

        // When
        Message message = Message.createUserMessage(1L, maxLengthContent);

        // Then
        assertThat(message.getContent()).isEqualTo(maxLengthContent);
        assertThat(message.getContent()).hasSize(10000);
    }
}