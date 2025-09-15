package com.chatgemma.repository;

import com.chatgemma.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(Long userId);

    Page<Chat> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    List<Chat> findByUserId(Long userId);

    Optional<Chat> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    long countByUserIdAndDeletedFalse(Long userId);

    long countByUserId(Long userId);

    List<Chat> findByUserIdAndTitleContainingAndDeletedFalse(Long userId, String titleKeyword);

    @Query("SELECT c FROM Chat c WHERE c.deleted = false ORDER BY c.updatedAt DESC")
    List<Chat> findRecentActiveChats(Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
}