package com.fashionshop.controller.admin;

import com.fashionshop.dto.ChatConversationRow;
import com.fashionshop.dto.ChatMessageJson;
import com.fashionshop.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/admin/chat")
public class AdminChatController {

    private final ChatService chatService;

    public AdminChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("conversations", chatService.listConversationsForAdmin());
        return "admin/chat-list";
    }

    @GetMapping("/{conversationId}")
    public String thread(@PathVariable Long conversationId, Model model) {
        ChatConversationRow row = chatService.findConversationById(conversationId).orElse(null);
        if (row == null) {
            return "redirect:/admin/chat";
        }
        model.addAttribute("conversation", row);
        model.addAttribute("messages", chatService.getMessagesForConversation(conversationId));
        return "admin/chat-thread";
    }

    @GetMapping(value = "/{conversationId}/api/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ChatMessageJson> messagesApi(@PathVariable Long conversationId) {
        if (!chatService.findConversationById(conversationId).isPresent()) {
            return List.of();
        }
        return chatService.getMessagesForConversation(conversationId);
    }

    @PostMapping("/{conversationId}/send")
    public String send(@PathVariable Long conversationId,
                       @RequestParam("content") String content,
                       Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        if (chatService.findConversationById(conversationId).isEmpty()) {
            return "redirect:/admin/chat";
        }
        chatService.sendAdminMessage(authentication.getName(), conversationId, content);
        return "redirect:/admin/chat/" + conversationId;
    }
}
