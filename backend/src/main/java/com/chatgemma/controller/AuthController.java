package com.chatgemma.controller;

import com.chatgemma.dto.request.LoginRequest;
import com.chatgemma.dto.request.RegisterRequest;
import com.chatgemma.dto.response.UserResponse;
import com.chatgemma.entity.User;
import com.chatgemma.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            User user = authService.register(request.getUsername(), request.getPassword(), request.getEmail(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));


            return ResponseEntity.ok("회원가입이 완료되었습니다. 관리자 승인을 기다려주세요.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpSession session) {
        try {
            User user = authService.login(request.getEmail(), request.getPassword(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            session.setAttribute("userId", user.getId());
            session.setAttribute("userRole", user.getRole());


            return ResponseEntity.ok(new UserResponse(user));
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session, HttpServletRequest httpRequest) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            authService.logout(userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        }

        session.invalidate();
        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User user = authService.getUserById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            return ResponseEntity.ok(new UserResponse(user));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}