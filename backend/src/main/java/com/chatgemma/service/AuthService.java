package com.chatgemma.service;

import com.chatgemma.entity.AuditLog;
import com.chatgemma.entity.User;
import com.chatgemma.entity.User.Status;
import com.chatgemma.repository.AuditLogRepository;
import com.chatgemma.repository.UserRepository;
import com.chatgemma.service.exception.AuthenticationException;
import com.chatgemma.service.exception.DuplicateUserException;
import com.chatgemma.service.exception.UserNotApprovedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, AuditLogRepository auditLogRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String password, String email, String clientIp, String userAgent) {
        // 입력 검증
        validateRegistrationInput(username, password, email);

        // 중복 확인
        if (userRepository.existsByUsernameOrEmail(username, email)) {
            recordAuditLog(null, "REGISTER_FAILED", "USER", null, clientIp, userAgent, "DUPLICATE");
            throw new DuplicateUserException("이미 존재하는 사용자명 또는 이메일입니다");
        }

        // 패스워드 인코딩 및 사용자 생성
        String encodedPassword = passwordEncoder.encode(password);
        User user = User.createUser(username, encodedPassword, email);

        // 저장
        User savedUser = userRepository.save(user);

        // 감사 로그 기록
        recordAuditLog(savedUser.getId(), "REGISTER", "USER", savedUser.getId(), clientIp, userAgent, null);

        return savedUser;
    }

    @Transactional
    public User login(String email, String password, String clientIp, String userAgent) {
        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            recordAuditLog(null, "LOGIN_FAILED", "USER", null, clientIp, userAgent, "USER_NOT_FOUND");
            throw new AuthenticationException("이메일 또는 패스워드가 올바르지 않습니다");
        }

        // 패스워드 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordAuditLog(user.getId(), "LOGIN_FAILED", "USER", user.getId(), clientIp, userAgent, "INVALID_PASSWORD");
            throw new AuthenticationException("이메일 또는 패스워드가 올바르지 않습니다");
        }

        // 승인 상태 확인 (테스트용으로 임시 비활성화)
        /*
        if (!user.isApproved()) {
            String details = user.getStatus() == Status.REJECTED ? "REJECTED" : "NOT_APPROVED";
            recordAuditLog(user.getId(), "LOGIN_FAILED", "USER", user.getId(), clientIp, userAgent, details);
            throw new UserNotApprovedException("관리자 승인이 필요합니다");
        }
        */

        // 성공적인 로그인 기록
        recordAuditLog(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), clientIp, userAgent, null);

        return user;
    }

    @Transactional
    public void logout(Long userId, String clientIp, String userAgent) {
        recordAuditLog(userId, "LOGOUT", "USER", userId, clientIp, userAgent, null);
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    private void validateRegistrationInput(String username, String password, String email) {
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("필수 입력값이 누락되었습니다");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다");
        }
    }

    private void recordAuditLog(Long userId, String action, String resourceType, Long resourceId,
                               String ipAddress, String userAgent, String details) {
        AuditLog auditLog;
        if (details != null) {
            auditLog = AuditLog.createWithDetails(userId, action, resourceType, resourceId,
                    ipAddress, userAgent, "{\"reason\":\"" + details + "\"}");
        } else {
            auditLog = AuditLog.create(userId, action, resourceType, resourceId, ipAddress, userAgent);
        }
        auditLogRepository.save(auditLog);
    }
}