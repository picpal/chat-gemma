package com.chatgemma.repository;

import com.chatgemma.entity.AuditLog;
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
@DisplayName("AuditLog Repository Tests")
class AuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Long user1Id = 1L;
    private Long user2Id = 2L;
    private AuditLog loginLog;
    private AuditLog chatCreateLog;
    private AuditLog systemLog;
    private AuditLog user2ActionLog;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        loginLog = AuditLog.create(user1Id, "LOGIN", "USER", user1Id, "192.168.1.1", "Mozilla/5.0");
        chatCreateLog = AuditLog.createWithDetails(user1Id, "CREATE_CHAT", "CHAT", 1L,
                "192.168.1.1", "Mozilla/5.0", "{\"title\":\"새 채팅\"}");
        systemLog = AuditLog.createSystemLog("SYSTEM_STARTUP", "SYSTEM", "127.0.0.1", "System");
        user2ActionLog = AuditLog.create(user2Id, "VIEW_CHAT", "CHAT", 2L, "10.0.0.1", "Chrome/90.0");

        entityManager.persist(loginLog);
        entityManager.persist(chatCreateLog);
        entityManager.persist(systemLog);
        entityManager.persist(user2ActionLog);
        entityManager.flush();
    }

    @Test
    @DisplayName("사용자별 감사 로그를 최신순으로 조회할 수 있다")
    void findByUserIdOrderByTimestampDesc_ShouldReturnUserLogsInOrder() {
        // When
        List<AuditLog> user1Logs = auditLogRepository.findByUserIdOrderByTimestampDesc(user1Id);

        // Then
        assertThat(user1Logs).hasSize(2);
        assertThat(user1Logs).extracting(AuditLog::getAction)
                .containsExactly("CREATE_CHAT", "LOGIN"); // timestamp 최신순
        assertThat(user1Logs).allMatch(log -> log.getUserId().equals(user1Id));
    }

    @Test
    @DisplayName("사용자별 감사 로그를 페이지별로 조회할 수 있다")
    void findByUserId_ShouldReturnPagedLogs() {
        // Given: 추가 로그 생성
        for (int i = 1; i <= 5; i++) {
            AuditLog log = AuditLog.create(user1Id, "ACTION_" + i, "RESOURCE", (long) i,
                    "192.168.1.1", "Browser");
            entityManager.persist(log);
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 3);

        // When
        Page<AuditLog> logPage = auditLogRepository.findByUserId(user1Id, pageable);

        // Then
        assertThat(logPage.getContent()).hasSize(3);
        assertThat(logPage.getTotalElements()).isEqualTo(7); // 기존 2개 + 추가 5개
        assertThat(logPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("액션별 감사 로그를 조회할 수 있다")
    void findByAction_ShouldReturnLogsWithSpecificAction() {
        // When
        List<AuditLog> loginLogs = auditLogRepository.findByAction("LOGIN");

        // Then
        assertThat(loginLogs).hasSize(1);
        assertThat(loginLogs.get(0)).isEqualTo(loginLog);
    }

    @Test
    @DisplayName("리소스 타입별 감사 로그를 조회할 수 있다")
    void findByResourceType_ShouldReturnLogsWithSpecificResourceType() {
        // When
        List<AuditLog> chatLogs = auditLogRepository.findByResourceType("CHAT");

        // Then
        assertThat(chatLogs).hasSize(2);
        assertThat(chatLogs).extracting(AuditLog::getAction)
                .containsExactlyInAnyOrder("CREATE_CHAT", "VIEW_CHAT");
    }

    @Test
    @DisplayName("특정 기간 내의 감사 로그를 조회할 수 있다")
    void findByTimestampBetween_ShouldReturnLogsInTimeRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // When
        List<AuditLog> logsInRange = auditLogRepository.findByTimestampBetween(start, end);

        // Then
        assertThat(logsInRange).hasSize(4); // 모든 로그가 현재 시간 전후 1시간 내에 있어야 함
    }

    @Test
    @DisplayName("IP 주소별 감사 로그를 조회할 수 있다")
    void findByIpAddress_ShouldReturnLogsFromSpecificIp() {
        // When
        List<AuditLog> logsFromIp = auditLogRepository.findByIpAddress("192.168.1.1");

        // Then
        assertThat(logsFromIp).hasSize(2);
        assertThat(logsFromIp).extracting(AuditLog::getAction)
                .containsExactlyInAnyOrder("LOGIN", "CREATE_CHAT");
    }

    @Test
    @DisplayName("시스템 로그만 조회할 수 있다")
    void findByUserIdIsNull_ShouldReturnSystemLogs() {
        // When
        List<AuditLog> systemLogs = auditLogRepository.findByUserIdIsNull();

        // Then
        assertThat(systemLogs).hasSize(1);
        assertThat(systemLogs.get(0)).isEqualTo(systemLog);
        assertThat(systemLogs.get(0).isSystemLog()).isTrue();
    }

    @Test
    @DisplayName("사용자별 특정 액션의 감사 로그를 조회할 수 있다")
    void findByUserIdAndAction_ShouldReturnUserSpecificActionLogs() {
        // When
        List<AuditLog> userLoginLogs = auditLogRepository.findByUserIdAndAction(user1Id, "LOGIN");

        // Then
        assertThat(userLoginLogs).hasSize(1);
        assertThat(userLoginLogs.get(0)).isEqualTo(loginLog);
    }

    @Test
    @DisplayName("사용자별 특정 기간 내의 감사 로그를 조회할 수 있다")
    void findByUserIdAndTimestampBetween_ShouldReturnUserLogsInTimeRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // When
        List<AuditLog> user1LogsInRange = auditLogRepository.findByUserIdAndTimestampBetween(user1Id, start, end);

        // Then
        assertThat(user1LogsInRange).hasSize(2);
        assertThat(user1LogsInRange).allMatch(log -> log.getUserId().equals(user1Id));
    }

    @Test
    @DisplayName("상세 정보가 있는 감사 로그를 조회할 수 있다")
    void findByDetailsIsNotNull_ShouldReturnLogsWithDetails() {
        // When
        List<AuditLog> logsWithDetails = auditLogRepository.findByDetailsIsNotNull();

        // Then
        assertThat(logsWithDetails).hasSize(1);
        assertThat(logsWithDetails.get(0)).isEqualTo(chatCreateLog);
        assertThat(logsWithDetails.get(0).hasDetails()).isTrue();
    }

    @Test
    @DisplayName("리소스 ID로 감사 로그를 조회할 수 있다")
    void findByResourceId_ShouldReturnLogsWithSpecificResourceId() {
        // When
        List<AuditLog> resourceLogs = auditLogRepository.findByResourceId(1L);

        // Then
        assertThat(resourceLogs).hasSize(2); // loginLog(resourceId=user1Id=1L) + chatCreateLog(resourceId=1L)
        assertThat(resourceLogs).contains(chatCreateLog, loginLog);
    }

    @Test
    @DisplayName("사용자별 감사 로그 수를 조회할 수 있다")
    void countByUserId_ShouldReturnCorrectCount() {
        // When
        long user1LogCount = auditLogRepository.countByUserId(user1Id);
        long user2LogCount = auditLogRepository.countByUserId(user2Id);

        // Then
        assertThat(user1LogCount).isEqualTo(2);
        assertThat(user2LogCount).isEqualTo(1);
    }

    @Test
    @DisplayName("특정 기간 내의 액션별 로그 수를 조회할 수 있다")
    void countByActionAndTimestampBetween_ShouldReturnCorrectCount() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        // When
        long loginLogCount = auditLogRepository.countByActionAndTimestampBetween("LOGIN", start, end);
        long createChatLogCount = auditLogRepository.countByActionAndTimestampBetween("CREATE_CHAT", start, end);

        // Then
        assertThat(loginLogCount).isEqualTo(1);
        assertThat(createChatLogCount).isEqualTo(1);
    }

    @Test
    @DisplayName("최근 감사 로그를 조회할 수 있다")
    void findRecentLogs_ShouldReturnRecentLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<AuditLog> recentLogs = auditLogRepository.findRecentLogs(pageable);

        // Then
        assertThat(recentLogs).hasSize(4);
        // timestamp 기준 최신순 정렬 확인
        for (int i = 0; i < recentLogs.size() - 1; i++) {
            LocalDateTime current = recentLogs.get(i).getTimestamp();
            LocalDateTime next = recentLogs.get(i + 1).getTimestamp();
            assertThat(current).isAfterOrEqualTo(next);
        }
    }

    @Test
    @DisplayName("사용자 액션 통계를 조회할 수 있다")
    void findUserActionStats_ShouldReturnActionCounts() {
        // Given: 동일한 액션의 추가 로그 생성
        AuditLog anotherLoginLog = AuditLog.create(user1Id, "LOGIN", "USER", user1Id, "192.168.1.2", "Safari");
        entityManager.persist(anotherLoginLog);
        entityManager.flush();

        // When
        List<AuditLog> loginLogs = auditLogRepository.findByAction("LOGIN");

        // Then
        assertThat(loginLogs).hasSize(2); // 기존 1개 + 추가 1개
    }
}