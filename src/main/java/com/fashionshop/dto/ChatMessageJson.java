package com.fashionshop.dto;

public record ChatMessageJson(
        long id,
        String authorName,
        boolean fromAdmin,
        String content,
        String createdAt
) {}
