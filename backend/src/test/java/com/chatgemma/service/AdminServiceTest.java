package com.chatgemma.service;

import com.chatgemma.entity.User;
import com.chatgemma.entity.User.Role;
import com.chatgemma.entity.User.Status;
import com.chatgemma.repository.AuditLogRepository;
import com.chatgemma.repository.UserRepository;
import com.chatgemma.service.exception.UnauthorizedAccessException;
import com.chatgemma.service.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;
    private User pendingUser;
    private User anotherPendingUser;
    private Long adminId = 1L;
    private String clientIp = "192.168.1.1";
    private String userAgent = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        adminUser = User.createAdmin("admin", "password", "admin@example.com");
        setUserId(adminUser, adminId);

        pendingUser = User.createUser("pending_user", "password", "pending@example.com");
        setUserId(pendingUser, 2L);

        anotherPendingUser = User.createUser("another_pending", "password", "another@example.com");
        setUserId(anotherPendingUser, 3L);
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("관리자가 승인 대기 중인 사용자 목록을 조회할 수 있다")
    void getPendingUsers_ShouldReturnPendingUsersList() {
        // Given
        List<User> pendingUsers = List.of(pendingUser, anotherPendingUser);
        when(userRepository.findPendingUsersOrderByCreatedAt()).thenReturn(pendingUsers);

        // When
        List<User> result = adminService.getPendingUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(pendingUser, anotherPendingUser);
        verify(userRepository).findPendingUsersOrderByCreatedAt();
    }

    @Test
    @DisplayName("관리자가 사용자를 승인할 수 있다")
    void approveUser_ShouldApproveUser_WhenValidRequest() {
        // Given
        Long targetUserId = 2L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = adminService.approveUser(adminId, targetUserId, clientIp, userAgent);

        // Then
        assertThat(result.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(result.getApprovedBy()).isEqualTo(adminId);
        assertThat(result.getApprovedAt()).isNotNull();

        verify(userRepository).save(pendingUser);
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("APPROVE_USER") &&
            log.getUserId().equals(adminId) &&
            log.getResourceId().equals(targetUserId)
        ));
    }

    @Test
    @DisplayName("관리자가 사용자를 거부할 수 있다")
    void rejectUser_ShouldRejectUser_WhenValidRequest() {
        // Given
        Long targetUserId = 2L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = adminService.rejectUser(adminId, targetUserId, clientIp, userAgent);

        // Then
        assertThat(result.getStatus()).isEqualTo(Status.REJECTED);
        assertThat(result.getApprovedBy()).isEqualTo(adminId);
        assertThat(result.getApprovedAt()).isNotNull();

        verify(userRepository).save(pendingUser);
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("REJECT_USER") &&
            log.getUserId().equals(adminId) &&
            log.getResourceId().equals(targetUserId)
        ));
    }

    @Test
    @DisplayName("관리자가 아닌 사용자가 승인을 시도하면 예외가 발생한다")
    void approveUser_ShouldThrowException_WhenNotAdmin() {
        // Given
        User regularUser = User.createUser("regular", "password", "regular@example.com");
        setUserId(regularUser, 10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(regularUser));

        // When & Then
        assertThatThrownBy(() -> adminService.approveUser(10L, 2L, clientIp, userAgent))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("관리자 권한이 필요합니다");

        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("UNAUTHORIZED_ACCESS") &&
            log.getDetails().contains("APPROVE_USER_ATTEMPT")
        ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 승인하려고 하면 예외가 발생한다")
    void approveUser_ShouldThrowException_WhenUserNotFound() {
        // Given
        Long nonExistentUserId = 999L;
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminService.approveUser(adminId, nonExistentUserId, clientIp, userAgent))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("이미 승인된 사용자를 다시 승인하려고 하면 예외가 발생한다")
    void approveUser_ShouldThrowException_WhenAlreadyApproved() {
        // Given
        User approvedUser = User.createUser("approved", "password", "approved@example.com");
        setUserId(approvedUser, 4L);
        approvedUser.approve(1L);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(approvedUser));

        // When & Then
        assertThatThrownBy(() -> adminService.approveUser(adminId, 4L, clientIp, userAgent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 사용자입니다");
    }

    @Test
    @DisplayName("모든 사용자 목록을 조회할 수 있다")
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> allUsers = List.of(adminUser, pendingUser, anotherPendingUser);
        when(userRepository.findAll()).thenReturn(allUsers);

        // When
        List<User> result = adminService.getAllUsers();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(adminUser, pendingUser, anotherPendingUser);
    }

    @Test
    @DisplayName("상태별로 사용자를 조회할 수 있다")
    void getUsersByStatus_ShouldReturnFilteredUsers() {
        // Given
        List<User> pendingUsers = List.of(pendingUser, anotherPendingUser);
        when(userRepository.findByStatus(Status.PENDING)).thenReturn(pendingUsers);

        // When
        List<User> result = adminService.getUsersByStatus(Status.PENDING);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> user.getStatus() == Status.PENDING);
    }

    @Test
    @DisplayName("역할별로 사용자를 조회할 수 있다")
    void getUsersByRole_ShouldReturnFilteredUsers() {
        // Given
        List<User> adminUsers = List.of(adminUser);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(adminUsers);

        // When
        List<User> result = adminService.getUsersByRole(Role.ADMIN);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("사용자 통계를 조회할 수 있다")
    void getUserStatistics_ShouldReturnStatistics() {
        // Given
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByStatus(Status.PENDING)).thenReturn(3L);
        when(userRepository.countByStatus(Status.APPROVED)).thenReturn(6L);
        when(userRepository.countByStatus(Status.REJECTED)).thenReturn(1L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userRepository.countByRole(Role.USER)).thenReturn(8L);

        // When
        AdminService.UserStatistics stats = adminService.getUserStatistics();

        // Then
        assertThat(stats.getTotalUsers()).isEqualTo(10);
        assertThat(stats.getPendingUsers()).isEqualTo(3);
        assertThat(stats.getApprovedUsers()).isEqualTo(6);
        assertThat(stats.getRejectedUsers()).isEqualTo(1);
        assertThat(stats.getAdminCount()).isEqualTo(2);
        assertThat(stats.getUserCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("관리자가 사용자를 관리자로 승격시킬 수 있다")
    void promoteToAdmin_ShouldChangeRoleToAdmin() {
        // Given
        User approvedUser = User.createUser("approved", "password", "approved@example.com");
        setUserId(approvedUser, 4L);
        approvedUser.approve(1L);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(approvedUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = adminService.promoteToAdmin(adminId, 4L, clientIp, userAgent);

        // Then
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(approvedUser);
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("PROMOTE_TO_ADMIN") &&
            log.getResourceId().equals(4L)
        ));
    }

    @Test
    @DisplayName("승인되지 않은 사용자를 관리자로 승격시키려고 하면 예외가 발생한다")
    void promoteToAdmin_ShouldThrowException_WhenUserNotApproved() {
        // Given
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));

        // When & Then
        assertThatThrownBy(() -> adminService.promoteToAdmin(adminId, 2L, clientIp, userAgent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("승인된 사용자만 관리자로 승격할 수 있습니다");
    }

    @Test
    @DisplayName("일괄 승인 기능이 정상 동작한다")
    void bulkApprove_ShouldApproveMultipleUsers() {
        // Given
        List<Long> userIds = List.of(2L, 3L);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherPendingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<User> result = adminService.bulkApprove(adminId, userIds, clientIp, userAgent);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> user.getStatus() == Status.APPROVED);
        verify(userRepository, times(2)).save(any(User.class));
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("BULK_APPROVE") &&
            log.getDetails().contains("count\":2")
        ));
    }
}