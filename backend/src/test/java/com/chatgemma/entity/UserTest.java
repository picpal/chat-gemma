package com.chatgemma.entity;

import com.chatgemma.entity.User.Role;
import com.chatgemma.entity.User.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

    @Test
    @DisplayName("사용자 생성 시 기본값이 올바르게 설정된다")
    void createUser_ShouldSetDefaultValues() {
        // Given
        String username = "testuser";
        String password = "password123";
        String email = "test@example.com";

        // When
        User user = User.createUser(username, password, email);

        // Then
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getStatus()).isEqualTo(Status.PENDING);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getApprovedAt()).isNull();
        assertThat(user.getApprovedBy()).isNull();
    }

    @Test
    @DisplayName("관리자 승인 시 상태와 승인 정보가 업데이트된다")
    void approve_ShouldUpdateStatusAndApprovalInfo() {
        // Given
        User user = User.createUser("testuser", "password123", "test@example.com");
        Long adminId = 1L;
        LocalDateTime beforeApproval = LocalDateTime.now();

        // When
        user.approve(adminId);

        // Then
        assertThat(user.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(user.getApprovedBy()).isEqualTo(adminId);
        assertThat(user.getApprovedAt()).isNotNull();
        assertThat(user.getApprovedAt()).isAfterOrEqualTo(beforeApproval);
    }

    @Test
    @DisplayName("관리자 거부 시 상태가 REJECTED로 변경된다")
    void reject_ShouldUpdateStatusToRejected() {
        // Given
        User user = User.createUser("testuser", "password123", "test@example.com");
        Long adminId = 1L;

        // When
        user.reject(adminId);

        // Then
        assertThat(user.getStatus()).isEqualTo(Status.REJECTED);
        assertThat(user.getApprovedBy()).isEqualTo(adminId);
        assertThat(user.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 승인된 사용자를 다시 승인하려고 하면 예외가 발생한다")
    void approve_AlreadyApprovedUser_ShouldThrowException() {
        // Given
        User user = User.createUser("testuser", "password123", "test@example.com");
        user.approve(1L);

        // When & Then
        assertThatThrownBy(() -> user.approve(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 사용자입니다");
    }

    @Test
    @DisplayName("이미 거부된 사용자를 승인하려고 하면 예외가 발생한다")
    void approve_AlreadyRejectedUser_ShouldThrowException() {
        // Given
        User user = User.createUser("testuser", "password123", "test@example.com");
        user.reject(1L);

        // When & Then
        assertThatThrownBy(() -> user.approve(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 사용자입니다");
    }

    @Test
    @DisplayName("승인된 사용자인지 확인한다")
    void isApproved_ShouldReturnCorrectStatus() {
        // Given
        User pendingUser = User.createUser("pending", "password", "pending@example.com");
        User approvedUser = User.createUser("approved", "password", "approved@example.com");
        approvedUser.approve(1L);
        User rejectedUser = User.createUser("rejected", "password", "rejected@example.com");
        rejectedUser.reject(1L);

        // When & Then
        assertThat(pendingUser.isApproved()).isFalse();
        assertThat(approvedUser.isApproved()).isTrue();
        assertThat(rejectedUser.isApproved()).isFalse();
    }

    @Test
    @DisplayName("관리자 사용자 생성 시 기본값이 올바르게 설정된다")
    void createAdmin_ShouldSetCorrectRoleAndStatus() {
        // Given
        String username = "admin";
        String password = "admin123";
        String email = "admin@example.com";

        // When
        User admin = User.createAdmin(username, password, email);

        // Then
        assertThat(admin.getUsername()).isEqualTo(username);
        assertThat(admin.getPassword()).isEqualTo(password);
        assertThat(admin.getEmail()).isEqualTo(email);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getApprovedAt()).isNotNull();
        assertThat(admin.getApprovedBy()).isNull(); // 자체 승인
    }

    @Test
    @DisplayName("사용자명이 null이면 예외가 발생한다")
    void createUser_WithNullUsername_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> User.createUser(null, "password", "email@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자명은 필수입니다");
    }

    @Test
    @DisplayName("사용자명이 빈 문자열이면 예외가 발생한다")
    void createUser_WithEmptyUsername_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> User.createUser("", "password", "email@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자명은 필수입니다");
    }

    @Test
    @DisplayName("패스워드가 null이면 예외가 발생한다")
    void createUser_WithNullPassword_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> User.createUser("username", null, "email@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("패스워드는 필수입니다");
    }

    @Test
    @DisplayName("이메일이 null이면 예외가 발생한다")
    void createUser_WithNullEmail_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> User.createUser("username", "password", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일은 필수입니다");
    }
}