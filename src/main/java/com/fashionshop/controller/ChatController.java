package com.fashionshop.controller;

import com.fashionshop.dto.ChatMessageJson;
import com.fashionshop.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping
    public String chatPage(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        if (isAdmin(authentication)) {
            return "redirect:/admin/chat";
        }
        return "user/chat";
    }

    @GetMapping(value = "/api/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ChatMessageJson> messagesApi(Authentication authentication) {
        if (authentication == null || isAdmin(authentication)) {
            return List.of();
        }
        return chatService.getMessagesForUser(authentication.getName());
    }

    @PostMapping("/send")
    public String send(@RequestParam("content") String content, Authentication authentication) {
        if (authentication != null && !isAdmin(authentication)) {
            chatService.sendUserMessage(authentication.getName(), content);
        }
        return "redirect:/chat";
    }
}
