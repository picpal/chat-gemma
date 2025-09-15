package com.chatgemma.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuditLog Entity Tests")
class AuditLogTest {

    @Test
    @DisplayName("감사 로그 생성 시 기본값이 올바르게 설정된다")
    void createAuditLog_ShouldSetDefaultValues() {
        // Given
        Long userId = 1L;
        String action = "LOGIN";
        String resourceType = "USER";
        Long resourceId = 1L;
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0 (Chrome)";

        // When
        AuditLog auditLog = AuditLog.create(userId, action, resourceType, resourceId, ipAddress, userAgent);

        // Then
        assertThat(auditLog.getUserId()).isEqualTo(userId);
        assertThat(auditLog.getAction()).isEqualTo(action);
        assertThat(auditLog.getResourceType()).isEqualTo(resourceType);
        assertThat(auditLog.getResourceId()).isEqualTo(resourceId);
        assertThat(auditLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(auditLog.getUserAgent()).isEqualTo(userAgent);
        assertThat(auditLog.getDetails()).isNull();
        assertThat(auditLog.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("상세 정보가 포함된 감사 로그 생성 시 모든 값이 올바르게 설정된다")
    void createAuditLogWithDetails_ShouldSetAllValues() {
        // Given
        Long userId = 1L;
        String action = "CREATE_CHAT";
        String resourceType = "CHAT";
        Long resourceId = 100L;
        String ipAddress = "10.0.0.1";
        String userAgent = "Mozilla/5.0 (Safari)";
        String details = "{\"chatTitle\":\"새 채팅\",\"messageCount\":0}";

        // When
        AuditLog auditLog = AuditLog.createWithDetails(userId, action, resourceType, resourceId, ipAddress, userAgent, details);

        // Then
        assertThat(auditLog.getUserId()).isEqualTo(userId);
        assertThat(auditLog.getAction()).isEqualTo(action);
        assertThat(auditLog.getResourceType()).isEqualTo(resourceType);
        assertThat(auditLog.getResourceId()).isEqualTo(resourceId);
        assertThat(auditLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(auditLog.getUserAgent()).isEqualTo(userAgent);
        assertThat(auditLog.getDetails()).isEqualTo(details);
        assertThat(auditLog.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("시스템 액션으로 감사 로그 생성 시 사용자 ID가 null이어도 성공한다")
    void createSystemAuditLog_WithNullUserId_ShouldSucceed() {
        // Given
        String action = "SYSTEM_STARTUP";
        String resourceType = "SYSTEM";
        String ipAddress = "127.0.0.1";
        String userAgent = "System";

        // When
        AuditLog auditLog = AuditLog.createSystemLog(action, resourceType, ipAddress, userAgent);

        // Then
        assertThat(auditLog.getUserId()).isNull();
        assertThat(auditLog.getAction()).isEqualTo(action);
        assertThat(auditLog.getResourceType()).isEqualTo(resourceType);
        assertThat(auditLog.getResourceId()).isNull();
        assertThat(auditLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(auditLog.getUserAgent()).isEqualTo(userAgent);
        assertThat(auditLog.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("액션이 null이면 예외가 발생한다")
    void createAuditLog_WithNullAction_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, null, "USER", 1L, "127.0.0.1", "Browser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("액션은 필수입니다");
    }

    @Test
    @DisplayName("액션이 빈 문자열이면 예외가 발생한다")
    void createAuditLog_WithEmptyAction_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, "", "USER", 1L, "127.0.0.1", "Browser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("액션은 필수입니다");
    }

    @Test
    @DisplayName("리소스 타입이 null이면 예외가 발생한다")
    void createAuditLog_WithNullResourceType_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, "LOGIN", null, 1L, "127.0.0.1", "Browser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리소스 타입은 필수입니다");
    }

    @Test
    @DisplayName("IP 주소가 null이면 예외가 발생한다")
    void createAuditLog_WithNullIpAddress_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, "LOGIN", "USER", 1L, null, "Browser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("IP 주소는 필수입니다");
    }

    @Test
    @DisplayName("User Agent가 null이면 예외가 발생한다")
    void createAuditLog_WithNullUserAgent_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, "LOGIN", "USER", 1L, "127.0.0.1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User Agent는 필수입니다");
    }

    @Test
    @DisplayName("액션 길이가 50자를 초과하면 예외가 발생한다")
    void createAuditLog_WithActionTooLong_ShouldThrowException() {
        // Given
        String longAction = "a".repeat(51);

        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, longAction, "USER", 1L, "127.0.0.1", "Browser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("액션은 50자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("리소스 타입 길이가 50자를 초과하면 예외가 발생한다")
    void createAuditLog_WithResourceTypeTooLong_ShouldThrowException() {
        // Given
        String longResourceType = "b".repeat(51);

        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, "LOGIN", longResourceType, 1L, "127.0.0.1", "Browser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리소스 타입은 50자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("User Agent 길이가 500자를 초과하면 예외가 발생한다")
    void createAuditLog_WithUserAgentTooLong_ShouldThrowException() {
        // Given
        String longUserAgent = "c".repeat(501);

        // When & Then
        assertThatThrownBy(() -> AuditLog.create(1L, "LOGIN", "USER", 1L, "127.0.0.1", longUserAgent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User Agent는 500자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("상세 정보 길이가 1000자를 초과하면 예외가 발생한다")
    void createAuditLogWithDetails_WithDetailsTooLong_ShouldThrowException() {
        // Given
        String longDetails = "d".repeat(1001);

        // When & Then
        assertThatThrownBy(() -> AuditLog.createWithDetails(1L, "LOGIN", "USER", 1L, "127.0.0.1", "Browser", longDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상세 정보는 1000자를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("시스템 로그인지 확인한다")
    void isSystemLog_ShouldReturnCorrectStatus() {
        // Given
        AuditLog userLog = AuditLog.create(1L, "LOGIN", "USER", 1L, "127.0.0.1", "Browser");
        AuditLog systemLog = AuditLog.createSystemLog("STARTUP", "SYSTEM", "127.0.0.1", "System");

        // When & Then
        assertThat(userLog.isSystemLog()).isFalse();
        assertThat(systemLog.isSystemLog()).isTrue();
    }

    @Test
    @DisplayName("상세 정보가 있는지 확인한다")
    void hasDetails_ShouldReturnCorrectStatus() {
        // Given
        AuditLog logWithoutDetails = AuditLog.create(1L, "LOGIN", "USER", 1L, "127.0.0.1", "Browser");
        AuditLog logWithDetails = AuditLog.createWithDetails(1L, "CREATE", "CHAT", 1L, "127.0.0.1", "Browser", "{\"test\":\"data\"}");

        // When & Then
        assertThat(logWithoutDetails.hasDetails()).isFalse();
        assertThat(logWithDetails.hasDetails()).isTrue();
    }

    @Test
    @DisplayName("경계값 길이의 필드들이 정확히 처리된다")
    void createAuditLog_WithMaxLengthFields_ShouldSucceed() {
        // Given
        String maxAction = "a".repeat(50);
        String maxResourceType = "b".repeat(50);
        String maxUserAgent = "c".repeat(500);
        String maxDetails = "d".repeat(1000);

        // When
        AuditLog auditLog = AuditLog.createWithDetails(1L, maxAction, maxResourceType, 1L, "127.0.0.1", maxUserAgent, maxDetails);

        // Then
        assertThat(auditLog.getAction()).hasSize(50);
        assertThat(auditLog.getResourceType()).hasSize(50);
        assertThat(auditLog.getUserAgent()).hasSize(500);
        assertThat(auditLog.getDetails()).hasSize(1000);
    }
}