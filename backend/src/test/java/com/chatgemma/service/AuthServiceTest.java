package com.chatgemma.service;

import com.chatgemma.entity.AuditLog;
import com.chatgemma.entity.User;
import com.chatgemma.entity.User.Role;
import com.chatgemma.entity.User.Status;
import com.chatgemma.repository.AuditLogRepository;
import com.chatgemma.repository.UserRepository;
import com.chatgemma.service.exception.AuthenticationException;
import com.chatgemma.service.exception.DuplicateUserException;
import com.chatgemma.service.exception.UserNotApprovedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User approvedUser;
    private User pendingUser;
    private User rejectedUser;
    private String clientIp = "192.168.1.1";
    private String userAgent = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        approvedUser = User.createUser("approved_user", "encoded_password", "approved@example.com");
        setUserId(approvedUser, 1L);
        approvedUser.approve(1L);

        pendingUser = User.createUser("pending_user", "encoded_password", "pending@example.com");
        setUserId(pendingUser, 2L);

        rejectedUser = User.createUser("rejected_user", "encoded_password", "rejected@example.com");
        setUserId(rejectedUser, 3L);
        rejectedUser.reject(1L);
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
    @DisplayName("신규 사용자 회원가입이 성공한다")
    void register_ShouldCreatePendingUser_WhenValidInput() {
        // Given
        String username = "newuser";
        String password = "password123";
        String email = "newuser@example.com";
        String encodedPassword = "encoded_password123";

        when(userRepository.existsByUsernameOrEmail(username, email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = authService.register(username, password, email, clientIp, userAgent);

        // Then
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        assertThat(result.getRole()).isEqualTo(Role.USER);

        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("REGISTER") &&
            log.getResourceType().equals("USER") &&
            log.getIpAddress().equals(clientIp)
        ));
    }

    @Test
    @DisplayName("중복된 사용자명으로 회원가입하면 예외가 발생한다")
    void register_ShouldThrowDuplicateUserException_WhenUsernameExists() {
        // Given
        String username = "existing_user";
        String email = "new@example.com";

        when(userRepository.existsByUsernameOrEmail(username, email)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(username, "password", email, clientIp, userAgent))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessage("이미 존재하는 사용자명 또는 이메일입니다");

        verify(userRepository, never()).save(any());
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("REGISTER_FAILED") &&
            log.getDetails().contains("DUPLICATE")
        ));
    }

    @Test
    @DisplayName("승인된 사용자의 로그인이 성공한다")
    void login_ShouldReturnUser_WhenValidCredentialsAndApproved() {
        // Given
        String username = "approved_user";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(approvedUser));
        when(passwordEncoder.matches(password, approvedUser.getPassword())).thenReturn(true);

        // When
        User result = authService.login(username, password, clientIp, userAgent);

        // Then
        assertThat(result).isEqualTo(approvedUser);
        assertThat(result.isApproved()).isTrue();

        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("LOGIN_SUCCESS") &&
            log.getUserId().equals(approvedUser.getId())
        ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인하면 예외가 발생한다")
    void login_ShouldThrowAuthenticationException_WhenUserNotFound() {
        // Given
        String username = "nonexistent_user";
        String password = "password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(username, password, clientIp, userAgent))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("사용자명 또는 패스워드가 올바르지 않습니다");

        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("LOGIN_FAILED") &&
            log.getUserId() == null &&
            log.getDetails().contains("USER_NOT_FOUND")
        ));
    }

    @Test
    @DisplayName("잘못된 패스워드로 로그인하면 예외가 발생한다")
    void login_ShouldThrowAuthenticationException_WhenPasswordIncorrect() {
        // Given
        String username = "approved_user";
        String wrongPassword = "wrong_password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(approvedUser));
        when(passwordEncoder.matches(wrongPassword, approvedUser.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(username, wrongPassword, clientIp, userAgent))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("사용자명 또는 패스워드가 올바르지 않습니다");

        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("LOGIN_FAILED") &&
            log.getUserId().equals(approvedUser.getId()) &&
            log.getDetails().contains("INVALID_PASSWORD")
        ));
    }

    @Test
    @DisplayName("승인 대기 중인 사용자로 로그인하면 예외가 발생한다")
    void login_ShouldThrowUserNotApprovedException_WhenUserPending() {
        // Given
        String username = "pending_user";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches(password, pendingUser.getPassword())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(username, password, clientIp, userAgent))
                .isInstanceOf(UserNotApprovedException.class)
                .hasMessage("관리자 승인이 필요합니다");

        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("LOGIN_FAILED") &&
            log.getUserId().equals(pendingUser.getId()) &&
            log.getDetails().contains("NOT_APPROVED")
        ));
    }

    @Test
    @DisplayName("거부된 사용자로 로그인하면 예외가 발생한다")
    void login_ShouldThrowUserNotApprovedException_WhenUserRejected() {
        // Given
        String username = "rejected_user";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(rejectedUser));
        when(passwordEncoder.matches(password, rejectedUser.getPassword())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(username, password, clientIp, userAgent))
                .isInstanceOf(UserNotApprovedException.class)
                .hasMessage("관리자 승인이 필요합니다");

        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("LOGIN_FAILED") &&
            log.getUserId().equals(rejectedUser.getId()) &&
            log.getDetails().contains("REJECTED")
        ));
    }

    @Test
    @DisplayName("로그아웃 시 감사 로그가 기록된다")
    void logout_ShouldRecordAuditLog() {
        // Given
        Long userId = 1L;

        // When
        authService.logout(userId, clientIp, userAgent);

        // Then
        verify(auditLogRepository).save(argThat(log ->
            log.getAction().equals("LOGOUT") &&
            log.getUserId().equals(userId) &&
            log.getResourceType().equals("USER") &&
            log.getIpAddress().equals(clientIp)
        ));
    }

    @Test
    @DisplayName("사용자명 중복 확인이 올바르게 동작한다")
    void isUsernameAvailable_ShouldReturnCorrectResult() {
        // Given
        String existingUsername = "existing_user";
        String newUsername = "new_user";

        when(userRepository.existsByUsername(existingUsername)).thenReturn(true);
        when(userRepository.existsByUsername(newUsername)).thenReturn(false);

        // When & Then
        assertThat(authService.isUsernameAvailable(existingUsername)).isFalse();
        assertThat(authService.isUsernameAvailable(newUsername)).isTrue();
    }

    @Test
    @DisplayName("이메일 중복 확인이 올바르게 동작한다")
    void isEmailAvailable_ShouldReturnCorrectResult() {
        // Given
        String existingEmail = "existing@example.com";
        String newEmail = "new@example.com";

        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);

        // When & Then
        assertThat(authService.isEmailAvailable(existingEmail)).isFalse();
        assertThat(authService.isEmailAvailable(newEmail)).isTrue();
    }

    @Test
    @DisplayName("사용자 ID로 사용자를 조회할 수 있다")
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(approvedUser));

        // When
        Optional<User> result = authService.getUserById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(approvedUser);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회하면 빈 Optional을 반환한다")
    void getUserById_ShouldReturnEmpty_WhenUserNotExists() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = authService.getUserById(userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("입력 검증 - null 사용자명으로 회원가입하면 예외가 발생한다")
    void register_ShouldThrowException_WhenUsernameIsNull() {
        // When & Then
        assertThatThrownBy(() -> authService.register(null, "password", "email@example.com", clientIp, userAgent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 입력값이 누락되었습니다");
    }

    @Test
    @DisplayName("입력 검증 - 빈 패스워드로 회원가입하면 예외가 발생한다")
    void register_ShouldThrowException_WhenPasswordIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> authService.register("username", "", "email@example.com", clientIp, userAgent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 입력값이 누락되었습니다");
    }

    @Test
    @DisplayName("입력 검증 - 유효하지 않은 이메일 형식으로 회원가입하면 예외가 발생한다")
    void register_ShouldThrowException_WhenEmailFormatInvalid() {
        // When & Then
        assertThatThrownBy(() -> authService.register("username", "password", "invalid-email", clientIp, userAgent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 이메일 형식입니다");
    }
}