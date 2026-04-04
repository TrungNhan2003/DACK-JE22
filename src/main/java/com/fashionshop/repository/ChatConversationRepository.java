package com.fashionshop.repository;

import com.fashionshop.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByUserId(Long userId);

    @Query("SELECT c FROM ChatConversation c JOIN FETCH c.user WHERE c.id = :id")
    Optional<ChatConversation> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT c FROM ChatConversation c JOIN FETCH c.user ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findAllOrderByLastMessageAtDescWithUser();
}
