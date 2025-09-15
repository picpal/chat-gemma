package com.chatgemma.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaServiceImpl Tests")
class OllamaServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private OllamaServiceImpl ollamaService;

    @BeforeEach
    void setUp() {
        ollamaService = new OllamaServiceImpl(webClient, "gemma3n:e4b", 60);
    }

    @Test
    @DisplayName("텍스트 메시지를 전송하고 응답을 받을 수 있다")
    void sendMessage_ShouldReturnResponse_WhenTextMessage() {
        // Given
        String message = "안녕하세요";
        String expectedResponse = "안녕하세요! 무엇을 도와드릴까요?";

        OllamaServiceImpl.OllamaResponse mockResponse = new OllamaServiceImpl.OllamaResponse();
        mockResponse.setResponse(expectedResponse);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/generate")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaServiceImpl.OllamaResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // When
        String result = ollamaService.sendMessage(message, null);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(requestBodySpec).bodyValue(argThat(body -> {
            OllamaServiceImpl.OllamaRequest request = (OllamaServiceImpl.OllamaRequest) body;
            return request.getPrompt().equals(message) &&
                   request.getModel().equals("gemma3n:e4b") &&
                   !request.isStream();
        }));
    }

    @Test
    @DisplayName("이미지가 포함된 메시지를 전송할 수 있다")
    void sendMessage_ShouldHandleImageMessage_WhenImageProvided() {
        // Given
        String message = "이 이미지를 설명해주세요";
        String imageUrl = "http://example.com/image.jpg";
        String expectedResponse = "이미지에는 고양이가 있습니다.";

        OllamaServiceImpl.OllamaResponse mockResponse = new OllamaServiceImpl.OllamaResponse();
        mockResponse.setResponse(expectedResponse);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/generate")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaServiceImpl.OllamaResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // When
        String result = ollamaService.sendMessage(message, imageUrl);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(requestBodySpec).bodyValue(argThat(body -> {
            OllamaServiceImpl.OllamaRequest request = (OllamaServiceImpl.OllamaRequest) body;
            return request.getPrompt().contains(message) &&
                   request.getPrompt().contains(imageUrl) &&
                   request.getModel().equals("gemma3n:e4b");
        }));
    }

    @Test
    @DisplayName("AI 서비스 오류 시 적절한 예외가 발생한다")
    void sendMessage_ShouldThrowException_WhenServiceError() {
        // Given
        String message = "테스트 메시지";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/generate")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaServiceImpl.OllamaResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        // When & Then
        assertThatThrownBy(() -> ollamaService.sendMessage(message, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI 서비스 연결 실패");
    }

    @Test
    @DisplayName("타임아웃이 발생하면 예외가 발생한다")
    void sendMessage_ShouldThrowException_WhenTimeout() {
        // Given
        String message = "테스트 메시지";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/generate")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OllamaServiceImpl.OllamaResponse.class))
                .thenReturn(Mono.never()); // 응답이 오지 않음

        // When & Then
        assertThatThrownBy(() -> ollamaService.sendMessage(message, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("빈 메시지를 전송하면 예외가 발생한다")
    void sendMessage_ShouldThrowException_WhenMessageIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> ollamaService.sendMessage("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메시지는 필수입니다");
    }

    @Test
    @DisplayName("null 메시지를 전송하면 예외가 발생한다")
    void sendMessage_ShouldThrowException_WhenMessageIsNull() {
        // When & Then
        assertThatThrownBy(() -> ollamaService.sendMessage(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메시지는 필수입니다");
    }
}