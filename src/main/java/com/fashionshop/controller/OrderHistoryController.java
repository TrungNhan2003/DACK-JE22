package com.fashionshop.controller;

import com.fashionshop.entity.Order;
import com.fashionshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order-history")
public class OrderHistoryController {

    private final OrderService orderService;

    @GetMapping
    public String orderHistory(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        model.addAttribute("orders", orderService.findByUserEmail(email));
        return "user/order-history";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId, Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Order order = orderService.findById(orderId).orElse(null);

        if (order != null && order.getUser() != null && order.getUser().getEmail().equals(email)) {
            model.addAttribute("order", order);
            return "user/order-detail";
        }

        return "redirect:/order-history";
    }
}
