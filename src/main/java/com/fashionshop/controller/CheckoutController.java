package com.fashionshop.controller;

import com.fashionshop.dto.CheckoutDTO;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.User;
import com.fashionshop.service.CartService;
import com.fashionshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public String showCheckout(Model model, Authentication authentication) {
        if (cartService.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }

        CheckoutDTO checkoutDTO = new CheckoutDTO();
        
        // Pre-fill user info if logged in
        if (authentication != null) {
            String email = authentication.getName();
            User user = orderService.findByEmail(email).orElse(null);
            if (user != null) {
                checkoutDTO.setFullName(user.getFullName());
                checkoutDTO.setPhone(user.getPhone() != null ? user.getPhone() : "");
                checkoutDTO.setAddress(user.getAddress() != null ? user.getAddress() : "");
            }
        }

        model.addAttribute("checkoutDTO", checkoutDTO);
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartTotal", cartService.getCartTotal());

        return "user/checkout";
    }

    @PostMapping
    public String processCheckout(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO dto,
                                  BindingResult result, Model model, Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("cartItems", cartService.getCartItems());
            model.addAttribute("cartTotal", cartService.getCartTotal());
            return "user/checkout";
        }

        if (cartService.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }

        String email = authentication != null ? authentication.getName() : null;
        Order order = orderService.createOrder(dto, email);

        if (order != null) {
            cartService.clearCart();
            // CheckoutController has base path "/checkout", so the success page is:
            // GET /checkout/order-success/{orderId}
            return "redirect:/checkout/order-success/" + order.getId();
        }

        return "redirect:/checkout?error";
    }

    @GetMapping("/order-success/{orderId}")
    public String orderSuccess(@PathVariable Long orderId, Model model) {
        Order order = orderService.findById(orderId).orElse(null);
        if (order != null) {
            model.addAttribute("order", order);
            return "user/order-success";
        }
        return "redirect:/";
    }
}
