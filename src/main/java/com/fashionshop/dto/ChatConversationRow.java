package com.fashionshop.dto;

import java.time.LocalDateTime;

public record ChatConversationRow(
        long id,
        String userName,
        String userEmail,
        LocalDateTime lastMessageAt
) {}
