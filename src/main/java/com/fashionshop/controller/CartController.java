package com.fashionshop.controller;

import com.fashionshop.entity.Product;
import com.fashionshop.service.CartService;
import com.fashionshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
                            @RequestParam(defaultValue = "1") Integer quantity) {
        Product product = productService.findById(productId).orElse(null);
        if (product != null) {
            cartService.addToCart(product, quantity);
        }
        return "redirect:/cart";
    }

    @PostMapping("/update/{productId}")
    public String updateCart(@PathVariable Long productId,
                             @RequestParam Integer quantity) {
        cartService.updateCartItem(productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart() {
        cartService.clearCart();
        return "redirect:/cart";
    }
}
