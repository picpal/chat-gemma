package com.chatgemma.repository;

import com.chatgemma.entity.User;
import com.chatgemma.entity.User.Role;
import com.chatgemma.entity.User.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("User Repository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User pendingUser;
    private User approvedUser;
    private User rejectedUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 준비
        pendingUser = User.createUser("pending_user", "password123", "pending@example.com");
        approvedUser = User.createUser("approved_user", "password123", "approved@example.com");
        approvedUser.approve(1L);
        rejectedUser = User.createUser("rejected_user", "password123", "rejected@example.com");
        rejectedUser.reject(1L);
        adminUser = User.createAdmin("admin", "admin123", "admin@example.com");

        entityManager.persist(pendingUser);
        entityManager.persist(approvedUser);
        entityManager.persist(rejectedUser);
        entityManager.persist(adminUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("사용자명으로 사용자를 찾을 수 있다")
    void findByUsername_ShouldReturnUser_WhenUsernameExists() {
        // When
        Optional<User> found = userRepository.findByUsername("approved_user");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("approved_user");
        assertThat(found.get().getStatus()).isEqualTo(Status.APPROVED);
    }

    @Test
    @DisplayName("존재하지 않는 사용자명으로 조회하면 빈 Optional을 반환한다")
    void findByUsername_ShouldReturnEmpty_WhenUsernameNotExists() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent_user");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("이메일로 사용자를 찾을 수 있다")
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        // When
        Optional<User> found = userRepository.findByEmail("admin@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("admin@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
    void findByEmail_ShouldReturnEmpty_WhenEmailNotExists() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("상태별로 사용자 목록을 조회할 수 있다")
    void findByStatus_ShouldReturnUsersWithSpecificStatus() {
        // When
        List<User> pendingUsers = userRepository.findByStatus(Status.PENDING);
        List<User> approvedUsers = userRepository.findByStatus(Status.APPROVED);
        List<User> rejectedUsers = userRepository.findByStatus(Status.REJECTED);

        // Then
        assertThat(pendingUsers).hasSize(1);
        assertThat(pendingUsers.get(0).getUsername()).isEqualTo("pending_user");

        assertThat(approvedUsers).hasSize(2); // approved_user + admin
        assertThat(approvedUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("approved_user", "admin");

        assertThat(rejectedUsers).hasSize(1);
        assertThat(rejectedUsers.get(0).getUsername()).isEqualTo("rejected_user");
    }

    @Test
    @DisplayName("역할별로 사용자 목록을 조회할 수 있다")
    void findByRole_ShouldReturnUsersWithSpecificRole() {
        // When
        List<User> users = userRepository.findByRole(Role.USER);
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        // Then
        assertThat(users).hasSize(3); // pending, approved, rejected users
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("pending_user", "approved_user", "rejected_user");

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("상태와 역할로 사용자 목록을 조회할 수 있다")
    void findByStatusAndRole_ShouldReturnUsersMatchingBothCriteria() {
        // When
        List<User> approvedUsers = userRepository.findByStatusAndRole(Status.APPROVED, Role.USER);
        List<User> approvedAdmins = userRepository.findByStatusAndRole(Status.APPROVED, Role.ADMIN);

        // Then
        assertThat(approvedUsers).hasSize(1);
        assertThat(approvedUsers.get(0).getUsername()).isEqualTo("approved_user");

        assertThat(approvedAdmins).hasSize(1);
        assertThat(approvedAdmins.get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("승인이 필요한 사용자 목록을 생성일 순으로 조회할 수 있다")
    void findPendingUsersOrderByCreatedAt_ShouldReturnPendingUsersInOrder() {
        // Given: 추가 pending 사용자 생성
        User anotherPendingUser = User.createUser("another_pending", "password", "another@example.com");
        entityManager.persist(anotherPendingUser);
        entityManager.flush();

        // When
        List<User> pendingUsers = userRepository.findPendingUsersOrderByCreatedAt();

        // Then
        assertThat(pendingUsers).hasSize(2);
        assertThat(pendingUsers).extracting(User::getUsername)
                .containsExactly("pending_user", "another_pending"); // 생성 순서대로
        assertThat(pendingUsers).allMatch(user -> user.getStatus() == Status.PENDING);
    }

    @Test
    @DisplayName("사용자명이나 이메일로 사용자 존재 여부를 확인할 수 있다")
    void existsByUsernameOrEmail_ShouldReturnTrue_WhenEitherExists() {
        // When & Then
        assertThat(userRepository.existsByUsernameOrEmail("approved_user", "any@example.com")).isTrue();
        assertThat(userRepository.existsByUsernameOrEmail("any_user", "admin@example.com")).isTrue();
        assertThat(userRepository.existsByUsernameOrEmail("approved_user", "admin@example.com")).isTrue();
        assertThat(userRepository.existsByUsernameOrEmail("nonexistent", "nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("활성 사용자 수를 조회할 수 있다")
    void countActiveUsers_ShouldReturnNumberOfApprovedUsers() {
        // When
        long activeUserCount = userRepository.countActiveUsers();

        // Then
        assertThat(activeUserCount).isEqualTo(2); // approved_user + admin
    }

    @Test
    @DisplayName("사용자명 중복 확인을 할 수 있다")
    void existsByUsername_ShouldReturnCorrectResult() {
        // When & Then
        assertThat(userRepository.existsByUsername("approved_user")).isTrue();
        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent_user")).isFalse();
    }

    @Test
    @DisplayName("이메일 중복 확인을 할 수 있다")
    void existsByEmail_ShouldReturnCorrectResult() {
        // When & Then
        assertThat(userRepository.existsByEmail("approved@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("admin@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("관리자 수를 조회할 수 있다")
    void countByRole_ShouldReturnCorrectCount() {
        // When
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long userCount = userRepository.countByRole(Role.USER);

        // Then
        assertThat(adminCount).isEqualTo(1);
        assertThat(userCount).isEqualTo(3);
    }
}