package com.chatgemma.repository;

import com.chatgemma.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByTimestampDesc(Long userId);

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByResourceType(String resourceType);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByIpAddress(String ipAddress);

    List<AuditLog> findByUserIdIsNull(); // 시스템 로그

    List<AuditLog> findByUserIdAndAction(Long userId, String action);

    List<AuditLog> findByUserIdAndTimestampBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByDetailsIsNotNull();

    List<AuditLog> findByResourceId(Long resourceId);

    long countByUserId(Long userId);

    long countByActionAndTimestampBetween(String action, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentLogs(Pageable pageable);
}