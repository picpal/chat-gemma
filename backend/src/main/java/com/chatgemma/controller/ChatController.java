package com.chatgemma.controller;

import com.chatgemma.dto.request.ChatRequest;
import com.chatgemma.dto.request.UpdateTitleRequest;
import com.chatgemma.dto.response.ChatResponse;
import com.chatgemma.dto.response.MessageResponse;
import com.chatgemma.entity.Chat;
import com.chatgemma.entity.Message;
import com.chatgemma.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(HttpSession session, HttpServletRequest httpRequest) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Chat chat = chatService.createChat(userId, "새 채팅", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));


            return ResponseEntity.ok(new ChatResponse(chat));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getUserChats(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Chat> chats = chatService.getUserActiveChats(userId);
            List<ChatResponse> chatResponses = chats.stream()
                    .map(ChatResponse::new)
                    .toList();

            return ResponseEntity.ok(chatResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChat(@PathVariable Long chatId, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Chat chat = chatService.getChatByIdAndUserId(chatId, userId);
            return ResponseEntity.ok(new ChatResponse(chat));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageResponse>> getChatMessages(@PathVariable Long chatId, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Message> messages = chatService.getChatMessages(chatId, userId);
            List<MessageResponse> messageResponses = messages.stream()
                    .map(MessageResponse::new)
                    .toList();

            return ResponseEntity.ok(messageResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(@PathVariable Long chatId,
                                                     @Valid @RequestBody ChatRequest request,
                                                     HttpSession session,
                                                     HttpServletRequest httpRequest) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Message userMessage = chatService.sendMessage(chatId, userId, request.getMessage(), request.getImageUrl(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

            // AI 응답을 위해 최신 메시지들을 조회하여 AI 응답 찾기
            List<Message> messages = chatService.getChatMessages(chatId, userId);
            Message aiMessage = messages.get(messages.size() - 1); // 마지막 메시지가 AI 응답


            return ResponseEntity.ok(new MessageResponse(aiMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{chatId}/title")
    public ResponseEntity<ChatResponse> updateChatTitle(@PathVariable Long chatId,
                                                       @Valid @RequestBody UpdateTitleRequest request,
                                                       HttpSession session,
                                                       HttpServletRequest httpRequest) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 디버깅을 위한 로그
            System.out.println("Received title: " + request.getTitle());
            Chat chat = chatService.updateChatTitle(chatId, userId, request.getTitle(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(new ChatResponse(chat));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<String> deleteChat(@PathVariable Long chatId,
                                           HttpSession session,
                                           HttpServletRequest httpRequest) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            chatService.deleteChat(chatId, userId, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));


            return ResponseEntity.ok("채팅이 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Long getUserIdFromSession(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
}