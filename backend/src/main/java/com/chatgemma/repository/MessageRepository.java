package com.chatgemma.repository;

import com.chatgemma.entity.Message;
import com.chatgemma.entity.Message.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatIdOrderByCreatedAtAsc(Long chatId);

    Page<Message> findByChatId(Long chatId, Pageable pageable);

    long countByChatId(Long chatId);

    Message findTopByChatIdOrderByCreatedAtDesc(Long chatId);

    long countByChatIdAndRole(Long chatId, Role role);

    List<Message> findByChatIdAndImageUrlIsNotNull(Long chatId);

    List<Message> findByChatIdAndCreatedAtAfter(Long chatId, LocalDateTime timestamp);

    List<Message> findByChatIdAndContentContaining(Long chatId, String keyword);

    Message findTopByChatIdOrderByCreatedAtAsc(Long chatId);

    void deleteByChatIdIn(List<Long> chatIds);

    List<Message> findByChatIdAndRole(Long chatId, Role role);
}