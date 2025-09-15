package com.chatgemma.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_log_user_id", columnList = "userId"),
        @Index(name = "idx_audit_log_action", columnList = "action"),
        @Index(name = "idx_audit_log_resource_type", columnList = "resourceType"),
        @Index(name = "idx_audit_log_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_log_ip_address", columnList = "ipAddress")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId; // null 허용 (시스템 액션의 경우)

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column
    private Long resourceId; // null 허용 (시스템 레벨 액션의 경우)

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 500)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 1000)
    private String details; // JSON 형태의 추가 정보

    protected AuditLog() {
        // JPA를 위한 기본 생성자
    }

    private AuditLog(Long userId, String action, String resourceType, Long resourceId,
                    String ipAddress, String userAgent, String details) {
        validateAction(action);
        validateResourceType(resourceType);
        validateRequired(ipAddress, "IP 주소는 필수입니다");
        validateUserAgent(userAgent);
        if (details != null) {
            validateDetails(details);
        }

        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public static AuditLog create(Long userId, String action, String resourceType, Long resourceId,
                                 String ipAddress, String userAgent) {
        return new AuditLog(userId, action, resourceType, resourceId, ipAddress, userAgent, null);
    }

    public static AuditLog createWithDetails(Long userId, String action, String resourceType, Long resourceId,
                                           String ipAddress, String userAgent, String details) {
        return new AuditLog(userId, action, resourceType, resourceId, ipAddress, userAgent, details);
    }

    public static AuditLog createSystemLog(String action, String resourceType, String ipAddress, String userAgent) {
        return new AuditLog(null, action, resourceType, null, ipAddress, userAgent, null);
    }

    public boolean isSystemLog() {
        return userId == null;
    }

    public boolean hasDetails() {
        return details != null && !details.trim().isEmpty();
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("액션은 필수입니다");
        }
        if (action.length() > 50) {
            throw new IllegalArgumentException("액션은 50자를 초과할 수 없습니다");
        }
    }

    private void validateResourceType(String resourceType) {
        if (resourceType == null || resourceType.trim().isEmpty()) {
            throw new IllegalArgumentException("리소스 타입은 필수입니다");
        }
        if (resourceType.length() > 50) {
            throw new IllegalArgumentException("리소스 타입은 50자를 초과할 수 없습니다");
        }
    }

    private void validateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            throw new IllegalArgumentException("User Agent는 필수입니다");
        }
        if (userAgent.length() > 500) {
            throw new IllegalArgumentException("User Agent는 500자를 초과할 수 없습니다");
        }
    }

    private void validateDetails(String details) {
        if (details.length() > 1000) {
            throw new IllegalArgumentException("상세 정보는 1000자를 초과할 수 없습니다");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id) &&
               Objects.equals(timestamp, auditLog.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId=" + resourceId +
                ", ipAddress='" + ipAddress + '\'' +
                ", timestamp=" + timestamp +
                ", hasDetails=" + hasDetails() +
                '}';
    }
}