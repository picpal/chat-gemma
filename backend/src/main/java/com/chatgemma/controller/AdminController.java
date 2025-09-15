package com.chatgemma.controller;

import com.chatgemma.dto.response.UserResponse;
import com.chatgemma.entity.User;
import com.chatgemma.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/pending-users")
    public ResponseEntity<List<UserResponse>> getPendingUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            List<User> pendingUsers = adminService.getPendingUsers();
            List<UserResponse> userResponses = pendingUsers.stream()
                    .map(UserResponse::new)
                    .toList();

            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<String> approveUser(@PathVariable Long userId,
                                            HttpSession session,
                                            HttpServletRequest httpRequest) {
        Long adminId = getUserIdFromSession(session);
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            adminService.approveUser(adminId, userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));


            return ResponseEntity.ok("사용자가 승인되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<String> rejectUser(@PathVariable Long userId,
                                           HttpSession session,
                                           HttpServletRequest httpRequest) {
        Long adminId = getUserIdFromSession(session);
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            adminService.rejectUser(adminId, userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));


            return ResponseEntity.ok("사용자가 거부되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/promote")
    public ResponseEntity<String> promoteToAdmin(@PathVariable Long userId,
                                               HttpSession session,
                                               HttpServletRequest httpRequest) {
        Long adminId = getUserIdFromSession(session);
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            adminService.promoteToAdmin(adminId, userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));


            return ResponseEntity.ok("사용자가 관리자로 승격되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/bulk-approve")
    public ResponseEntity<String> bulkApproveUsers(@RequestBody List<Long> userIds,
                                                 HttpSession session,
                                                 HttpServletRequest httpRequest) {
        Long adminId = getUserIdFromSession(session);
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            List<User> approvedUsers = adminService.bulkApprove(adminId, userIds, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            int approvedCount = approvedUsers.size();


            return ResponseEntity.ok(approvedCount + "명의 사용자가 승인되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<AdminService.UserStatistics> getUserStatistics(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            AdminService.UserStatistics statistics = adminService.getUserStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long getUserIdFromSession(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    private boolean isAdmin(HttpSession session) {
        User.Role userRole = (User.Role) session.getAttribute("userRole");
        return userRole != null && userRole == User.Role.ADMIN;
    }
}