package com.chatgemma.service;

import com.chatgemma.entity.AuditLog;
import com.chatgemma.entity.User;
import com.chatgemma.entity.User.Role;
import com.chatgemma.entity.User.Status;
import com.chatgemma.repository.AuditLogRepository;
import com.chatgemma.repository.UserRepository;
import com.chatgemma.service.exception.UnauthorizedAccessException;
import com.chatgemma.service.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<User> getPendingUsers() {
        return userRepository.findPendingUsersOrderByCreatedAt();
    }

    @Transactional
    public User approveUser(Long adminId, Long targetUserId, String clientIp, String userAgent) {
        // 관리자 권한 확인
        User admin = validateAdminAccess(adminId, "APPROVE_USER_ATTEMPT", clientIp, userAgent);

        // 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        // 승인 처리
        targetUser.approve(adminId);
        User approvedUser = userRepository.save(targetUser);

        // 감사 로그 기록
        recordAuditLog(adminId, "APPROVE_USER", "USER", targetUserId, clientIp, userAgent,
                "{\"username\":\"" + targetUser.getUsername() + "\"}");

        return approvedUser;
    }

    @Transactional
    public User rejectUser(Long adminId, Long targetUserId, String clientIp, String userAgent) {
        // 관리자 권한 확인
        User admin = validateAdminAccess(adminId, "REJECT_USER_ATTEMPT", clientIp, userAgent);

        // 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        // 거부 처리
        targetUser.reject(adminId);
        User rejectedUser = userRepository.save(targetUser);

        // 감사 로그 기록
        recordAuditLog(adminId, "REJECT_USER", "USER", targetUserId, clientIp, userAgent,
                "{\"username\":\"" + targetUser.getUsername() + "\"}");

        return rejectedUser;
    }

    @Transactional
    public User promoteToAdmin(Long adminId, Long targetUserId, String clientIp, String userAgent) {
        // 관리자 권한 확인
        User admin = validateAdminAccess(adminId, "PROMOTE_TO_ADMIN_ATTEMPT", clientIp, userAgent);

        // 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        // 승인된 사용자인지 확인
        if (!targetUser.isApproved()) {
            throw new IllegalStateException("승인된 사용자만 관리자로 승격할 수 있습니다");
        }

        // 관리자로 승격
        targetUser.setRole(Role.ADMIN);
        User promotedUser = userRepository.save(targetUser);

        // 감사 로그 기록
        recordAuditLog(adminId, "PROMOTE_TO_ADMIN", "USER", targetUserId, clientIp, userAgent,
                "{\"username\":\"" + targetUser.getUsername() + "\"}");

        return promotedUser;
    }

    @Transactional
    public List<User> bulkApprove(Long adminId, List<Long> userIds, String clientIp, String userAgent) {
        // 관리자 권한 확인
        User admin = validateAdminAccess(adminId, "BULK_APPROVE_ATTEMPT", clientIp, userAgent);

        List<User> approvedUsers = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

                if (user.getStatus() == Status.PENDING) {
                    user.approve(adminId);
                    User approved = userRepository.save(user);
                    approvedUsers.add(approved);
                }
            } catch (Exception e) {
                // 개별 사용자 처리 실패 시 로그만 남기고 계속 진행
                recordAuditLog(adminId, "BULK_APPROVE_FAILED", "USER", userId, clientIp, userAgent,
                        "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        // 일괄 승인 감사 로그 기록
        recordAuditLog(adminId, "BULK_APPROVE", "USER", null, clientIp, userAgent,
                "{\"count\":" + approvedUsers.size() + ",\"total\":" + userIds.size() + "}");

        return approvedUsers;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByStatus(Status status) {
        return userRepository.findByStatus(status);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public UserStatistics getUserStatistics() {
        UserStatistics stats = new UserStatistics();
        stats.totalUsers = userRepository.count();
        stats.pendingUsers = userRepository.countByStatus(Status.PENDING);
        stats.approvedUsers = userRepository.countByStatus(Status.APPROVED);
        stats.rejectedUsers = userRepository.countByStatus(Status.REJECTED);
        stats.adminCount = userRepository.countByRole(Role.ADMIN);
        stats.userCount = userRepository.countByRole(Role.USER);
        return stats;
    }

    private User validateAdminAccess(Long userId, String attemptAction, String clientIp, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        if (user.getRole() != Role.ADMIN) {
            // 권한 없는 접근 시도 로깅
            recordAuditLog(userId, "UNAUTHORIZED_ACCESS", "ADMIN", null, clientIp, userAgent,
                    "{\"attemptedAction\":\"" + attemptAction + "\"}");
            throw new UnauthorizedAccessException("관리자 권한이 필요합니다");
        }

        return user;
    }

    private void recordAuditLog(Long userId, String action, String resourceType, Long resourceId,
                               String ipAddress, String userAgent, String details) {
        AuditLog auditLog;
        if (details != null) {
            auditLog = AuditLog.createWithDetails(userId, action, resourceType, resourceId,
                    ipAddress, userAgent, details);
        } else {
            auditLog = AuditLog.create(userId, action, resourceType, resourceId, ipAddress, userAgent);
        }
        auditLogRepository.save(auditLog);
    }

    public static class UserStatistics {
        private long totalUsers;
        private long pendingUsers;
        private long approvedUsers;
        private long rejectedUsers;
        private long adminCount;
        private long userCount;

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getPendingUsers() { return pendingUsers; }
        public long getApprovedUsers() { return approvedUsers; }
        public long getRejectedUsers() { return rejectedUsers; }
        public long getAdminCount() { return adminCount; }
        public long getUserCount() { return userCount; }
    }
}