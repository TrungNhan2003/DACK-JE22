package com.fashionshop.service;

import com.fashionshop.dto.ChatConversationRow;
import com.fashionshop.dto.ChatMessageJson;

import java.util.List;
import java.util.Optional;

public interface ChatService {

    List<ChatMessageJson> getMessagesForUser(String userEmail);

    List<ChatMessageJson> getMessagesForConversation(Long conversationId);

    void sendUserMessage(String userEmail, String content);

    void sendAdminMessage(String adminEmail, Long conversationId, String content);

    List<ChatConversationRow> listConversationsForAdmin();

    Optional<ChatConversationRow> findConversationById(Long conversationId);
}
