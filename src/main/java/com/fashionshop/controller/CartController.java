package com.fashionshop.controller;

import com.fashionshop.entity.Product;
import com.fashionshop.service.CartService;
import com.fashionshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartTotal", cartService.getCartTotal());
        model.addAttribute("cartCount", cartService.getCartCount());
        return "user/cart";
    }

    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            RedirectAttributes redirectAttributes) {
        Product product = productService.findById(productId).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại.");
            return "redirect:/cart";
        }

        try {
            cartService.addToCart(product, quantity);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/update/{productId}")
    public String updateCart(@PathVariable Long productId,
                             @RequestParam Integer quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            cartService.updateCartItem(productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Cập nhật giỏ hàng thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/update-ajax/{productId}")
    @ResponseBody
    public ResponseEntity<?> updateCartAjax(@PathVariable Long productId,
                                            @RequestParam Integer quantity) {
        Map<String, Object> response = new HashMap<>();

        try {
            cartService.updateCartItem(productId, quantity);

            BigDecimal cartTotal = cartService.getCartTotal();
            int cartCount = cartService.getCartCount();

            response.put("success", true);
            response.put("cartTotal", cartTotal);
            response.put("cartCount", cartCount);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId,
                                 RedirectAttributes redirectAttributes) {
        cartService.removeFromCart(productId);
        redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng.");
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        cartService.clearCart();
        redirectAttributes.addFlashAttribute("success", "Đã xóa toàn bộ giỏ hàng.");
        return "redirect:/cart";
    }
}