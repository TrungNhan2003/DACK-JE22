package com.fashionshop.service;

import com.fashionshop.dto.ChatConversationRow;
import com.fashionshop.dto.ChatMessageJson;
import com.fashionshop.entity.ChatConversation;
import com.fashionshop.entity.ChatMessage;
import com.fashionshop.entity.User;
import com.fashionshop.repository.ChatConversationRepository;
import com.fashionshop.repository.ChatMessageRepository;
import com.fashionshop.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTENT = 2000;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatServiceImpl(ChatConversationRepository conversationRepository,
                           ChatMessageRepository messageRepository,
                           UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    private static String trimContent(String content) {
        if (content == null) {
            return "";
        }
        String t = content.trim();
        if (t.length() > MAX_CONTENT) {
            return t.substring(0, MAX_CONTENT);
        }
        return t;
    }

    private static ChatMessageJson toJson(ChatMessage m) {
        boolean fromAdmin = m.getAuthor().getRole() == User.Role.ROLE_ADMIN;
        return new ChatMessageJson(
                m.getId(),
                m.getAuthor().getFullName(),
                fromAdmin,
                m.getContent(),
                m.getCreatedAt() != null ? m.getCreatedAt().toString() : ""
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageJson> getMessagesForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return List.of();
        }
        return conversationRepository.findByUserId(user.getId())
                .map(c -> messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId())
                        .stream()
                        .map(ChatServiceImpl::toJson)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageJson> getMessagesForConversation(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(ChatServiceImpl::toJson)
                .collect(Collectors.toList());
    }

    private ChatConversation getOrCreateConversation(User user) {
        return conversationRepository.findByUserId(user.getId()).orElseGet(() -> {
            ChatConversation c = new ChatConversation();
            c.setUser(user);
            c.setLastMessageAt(LocalDateTime.now());
            return conversationRepository.save(c);
        });
    }

    @Override
    @Transactional
    public void sendUserMessage(String userEmail, String content) {
        String body = trimContent(content);
        if (body.isEmpty()) {
            return;
        }
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if (user.getRole() == User.Role.ROLE_ADMIN) {
            return;
        }
        ChatConversation conv = getOrCreateConversation(user);
        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setAuthor(user);
        msg.setContent(body);
        messageRepository.save(msg);
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);
    }

    @Override
    @Transactional
    public void sendAdminMessage(String adminEmail, Long conversationId, String content) {
        String body = trimContent(content);
        if (body.isEmpty()) {
            return;
        }
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        if (admin.getRole() != User.Role.ROLE_ADMIN) {
            return;
        }
        ChatConversation conv = conversationRepository.findById(conversationId).orElseThrow();
        ChatMessage msg = new ChatMessage();
        msg.setConversation(conv);
        msg.setAuthor(admin);
        msg.setContent(body);
        messageRepository.save(msg);
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatConversationRow> listConversationsForAdmin() {
        return conversationRepository.findAllOrderByLastMessageAtDescWithUser().stream()
                .map(c -> new ChatConversationRow(
                        c.getId(),
                        c.getUser().getFullName(),
                        c.getUser().getEmail(),
                        c.getLastMessageAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatConversationRow> findConversationById(Long conversationId) {
        return conversationRepository.findByIdWithUser(conversationId)
                .map(c -> new ChatConversationRow(
                        c.getId(),
                        c.getUser().getFullName(),
                        c.getUser().getEmail(),
                        c.getLastMessageAt()
                ));
    }
}
