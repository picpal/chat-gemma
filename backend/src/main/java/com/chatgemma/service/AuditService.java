package com.chatgemma.service;

import com.chatgemma.entity.AuditLog;
import com.chatgemma.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logUserAction(Long userId, String action, String resourceType, Long resourceId,
                            String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.create(userId, action, resourceType, resourceId, ipAddress, userAgent);
        auditLogRepository.save(auditLog);
    }

    public void logUserActionWithDetails(Long userId, String action, String resourceType, Long resourceId,
                                       String ipAddress, String userAgent, String details) {
        AuditLog auditLog = AuditLog.createWithDetails(userId, action, resourceType, resourceId,
                                                      ipAddress, userAgent, details);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }
}