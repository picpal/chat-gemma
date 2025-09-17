package com.chatgemma.service;

import com.chatgemma.entity.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

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

    @Override
    public String sendMessageWithContext(String message, String imageUrl, List<Message> recentMessages) {
        validateInput(message);

        String prompt = buildPromptWithContext(message, imageUrl, recentMessages);
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

    private String buildPromptWithContext(String message, String imageUrl, List<Message> recentMessages) {
        StringBuilder contextBuilder = new StringBuilder();

        // 대화 초기화 키워드 감지
        if (isContextResetRequest(message)) {
            contextBuilder.append("=== 대화 초기화 요청 ===\n");
            contextBuilder.append("이전 대화 내용을 모두 잊고 새로운 대화를 시작합니다.\n\n");
            contextBuilder.append("현재 질문: ").append(message).append("\n\n");
            contextBuilder.append("답변: 네, 이전 대화 내용을 모두 잊었습니다. 새로운 대화를 시작하겠습니다. 무엇을 도와드릴까요?");
            return contextBuilder.toString();
        }

        // 범용 고효율 프롬프트 시스템
        contextBuilder.append("=== Gemma 3n AI 어시스턴트 지침 ===\n");
        contextBuilder.append("모델: Google Gemma 3n (효율적 온디바이스 멀티모달 모델)\n");
        contextBuilder.append("역할: 친근하고 도움이 되는 한국어 전문 AI 어시스턴트\n\n");

        contextBuilder.append("핵심 원칙:\n");
        contextBuilder.append("• 정확하고 실용적인 정보를 간결하게 제공\n");
        contextBuilder.append("• 친근하고 자연스러운 한국어 대화 스타일 유지\n");
        contextBuilder.append("• 이전 대화 맥락을 적극 활용한 일관된 답변\n");
        contextBuilder.append("• 불확실한 정보는 명확히 구분하여 표시\n");
        contextBuilder.append("• 복잡한 내용은 단계별로 체계적으로 설명\n");
        contextBuilder.append("• 텍스트와 이미지를 함께 고려한 멀티모달 이해\n\n");

        contextBuilder.append("응답 가이드라인:\n");
        contextBuilder.append("• 사용자 의도를 정확히 파악하고 개인화된 답변 제공\n");
        contextBuilder.append("• 한국 문화와 언어 특성을 고려한 적절한 표현 사용\n");
        contextBuilder.append("• 필요시 구체적 예시나 친숙한 비유 활용\n");
        contextBuilder.append("• 추가 궁금증을 예상하고 관련 정보나 도움 제안\n");
        contextBuilder.append("• 같은 질문에는 항상 일관된 정보 제공\n");
        contextBuilder.append("• 온디바이스 환경의 장점(개인정보 보호, 빠른 응답)을 활용\n\n");

        // 스마트 컨텍스트 관리: 80% 토큰 사용량 (약 25,600 토큰)
        if (recentMessages != null && !recentMessages.isEmpty()) {
            contextBuilder.append("이전 대화 내용:\n");

            // 토큰 기반 동적 메시지 수 조절
            int maxMessages = calculateOptimalMessageCount(recentMessages);
            maxMessages = Math.min(recentMessages.size(), maxMessages);

            for (int i = recentMessages.size() - maxMessages; i < recentMessages.size(); i++) {
                Message msg = recentMessages.get(i);
                String roleLabel = msg.getRole() == Message.Role.USER ? "사용자" : "AI";

                // 긴 메시지는 요약
                String content = msg.getContent();
                if (content.length() > 200) {
                    content = content.substring(0, 197) + "...";
                }

                contextBuilder.append(String.format("%s: %s\n", roleLabel, content));

                if (msg.hasImage()) {
                    contextBuilder.append(String.format("  (이미지: %s)\n", msg.getImageUrl()));
                }
            }
            contextBuilder.append("\n");
        }

        // 현재 메시지 추가
        contextBuilder.append("현재 질문:\n");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            contextBuilder.append(String.format("이미지 URL: %s\n", imageUrl));
        }
        contextBuilder.append(message);

        contextBuilder.append("\n\n답변 시 위의 이전 대화를 참고하여 일관성 있게 답변하세요.");

        return contextBuilder.toString();
    }

    private int calculateOptimalMessageCount(List<Message> messages) {
        // 스마트 토큰 관리: 3단계 전략

        // 1. 기본 토큰 예산 (80% 사용)
        int targetTokens = 25600; // 32,000 × 0.8

        // 2. 메시지별 예상 토큰 계산
        int estimatedTokens = 0;
        int messageCount = 0;

        for (int i = messages.size() - 1; i >= 0 && estimatedTokens < targetTokens; i--) {
            Message msg = messages.get(i);
            int msgTokens = estimateTokenCount(msg.getContent());

            // 토큰 예산 초과 시 중단
            if (estimatedTokens + msgTokens > targetTokens) {
                break;
            }

            estimatedTokens += msgTokens;
            messageCount++;

            // 최대 70개 제한 (안전 마진)
            if (messageCount >= 70) {
                break;
            }
        }

        return messageCount;
    }

    private int estimateTokenCount(String content) {
        // 한국어 토큰 추정 알고리즘
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 기본 계산: 글자수 × 2.5 (한국어 평균)
        int baseTokens = (int) (content.length() * 2.5);

        // 특수문자, 숫자, 영어 고려한 보정
        long koreanChars = content.chars().filter(ch -> ch >= 0xAC00 && ch <= 0xD7AF).count();
        long otherChars = content.length() - koreanChars;

        // 한글: 2.5토큰/글자, 기타: 1.2토큰/글자
        return (int) (koreanChars * 2.5 + otherChars * 1.2);
    }

    private boolean isContextResetRequest(String message) {
        if (message == null) return false;

        String normalizedMessage = message.toLowerCase().trim();

        // 다양한 대화 초기화 키워드 패턴
        String[] resetKeywords = {
            "이전 대화 잊어버려", "이전 대화 잊어", "대화 잊어버려",
            "대화 내용 초기화", "대화 초기화", "컨텍스트 초기화",
            "새로 시작해", "새로 시작하자", "처음부터 시작",
            "리셋", "reset", "clear",
            "기억 지워", "기억 삭제", "잊어버려",
            "대화 지워", "히스토리 삭제", "이전 내용 삭제"
        };

        for (String keyword : resetKeywords) {
            if (normalizedMessage.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
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

    // WebSocket 스트리밍용 메소드
    public void sendMessageStream(String message, String imageUrl, Consumer<String> chunkConsumer) {
        try {
            String fullResponse = sendMessage(message, imageUrl);

            // 응답을 단어별로 나누어 스트리밍 시뮬레이션
            String[] words = fullResponse.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                // 마지막 단어가 아닌 경우에만 공백 추가
                String chunk = (i == words.length - 1) ? word : word + " ";
                chunkConsumer.accept(chunk);
                try {
                    Thread.sleep(50); // 50ms 간격으로 단어별 전송
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            chunkConsumer.accept("오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 컨텍스트를 포함한 WebSocket 스트리밍용 메소드
    public void sendMessageStreamWithContext(String message, String imageUrl, List<Message> recentMessages, Consumer<String> chunkConsumer) {
        try {
            String fullResponse = sendMessageWithContext(message, imageUrl, recentMessages);

            // 응답을 단어별로 나누어 스트리밍 시뮬레이션
            String[] words = fullResponse.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                // 마지막 단어가 아닌 경우에만 공백 추가
                String chunk = (i == words.length - 1) ? word : word + " ";
                chunkConsumer.accept(chunk);
                try {
                    Thread.sleep(50); // 50ms 간격으로 단어별 전송
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            chunkConsumer.accept("오류가 발생했습니다: " + e.getMessage());
        }
    }
}