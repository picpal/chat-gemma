package com.chatgemma.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Primary
public class OllamaServiceImpl implements OllamaService {

    private final WebClient webClient;
    private final String modelName;
    private final int timeoutSeconds;

    public OllamaServiceImpl(WebClient webClient,
                            @Value("${chatgemma.ollama.model-name:gemma3n:e4b}") String modelName,
                            @Value("${chatgemma.ollama.timeout:60}") int timeoutSeconds) {
        this.webClient = webClient;
        this.modelName = modelName;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String sendMessage(String message, String imageUrl) {
        validateInput(message);

        String prompt = buildPrompt(message, imageUrl);
        OllamaRequest request = new OllamaRequest(modelName, prompt);

        try {
            OllamaResponse response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null || response.getResponse() == null) {
                throw new RuntimeException("AI 서비스로부터 응답을 받지 못했습니다");
            }

            return response.getResponse();
        } catch (Exception e) {
            throw new RuntimeException("AI 서비스 연결 실패: " + e.getMessage(), e);
        }
    }

    private void validateInput(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지는 필수입니다");
        }
    }

    private String buildPrompt(String message, String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            // 이미지가 있는 경우 프롬프트에 이미지 컨텍스트 추가
            return String.format("이미지 URL: %s\n\n%s", imageUrl, message);
        }
        return message;
    }

    // Request/Response DTOs
    static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream = false;

        public OllamaRequest() {}

        public OllamaRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }

        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
    }

    static class OllamaResponse {
        private String model;
        private String response;

        @JsonProperty("created_at")
        private String createdAt;

        private boolean done;

        @JsonProperty("eval_count")
        private Integer evalCount;

        @JsonProperty("eval_duration")
        private Long evalDuration;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public boolean isDone() { return done; }
        public void setDone(boolean done) { this.done = done; }

        public Integer getEvalCount() { return evalCount; }
        public void setEvalCount(Integer evalCount) { this.evalCount = evalCount; }

        public Long getEvalDuration() { return evalDuration; }
        public void setEvalDuration(Long evalDuration) { this.evalDuration = evalDuration; }
    }
}