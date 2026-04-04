package com.fashionshop.controller;

import com.fashionshop.dto.CheckoutDTO;
import com.fashionshop.dto.VoucherPreviewResponse;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.User;
import com.fashionshop.service.CartService;
import com.fashionshop.service.OrderService;
import com.fashionshop.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final VoucherService voucherService;

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
        enrichCheckoutModel(model, checkoutDTO);

        return "user/checkout";
    }

    @GetMapping(value = "/api/voucher-preview", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public VoucherPreviewResponse voucherPreview(@RequestParam(required = false) String code) {
        BigDecimal subtotal = cartService.getCartTotal();
        return voucherService.preview(code, subtotal);
    }

    private void enrichCheckoutModel(Model model, CheckoutDTO dto) {
        BigDecimal cartTotal = cartService.getCartTotal();
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartTotal", cartTotal);
        VoucherService.DiscountResult vr = voucherService.apply(dto.getVoucherCode(), cartTotal);
        if (!vr.invalidCode() && vr.discountAmount() != null && vr.discountAmount().signum() > 0) {
            model.addAttribute("voucherDiscount", vr.discountAmount());
            model.addAttribute("finalTotal", cartTotal.subtract(vr.discountAmount()).max(BigDecimal.ZERO));
        } else {
            model.addAttribute("finalTotal", cartTotal);
        }
    }

    @PostMapping
    public String processCheckout(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO dto,
                                  BindingResult result, Model model, Authentication authentication) {
        BigDecimal cartTotal = cartService.getCartTotal();
        VoucherService.DiscountResult vr = voucherService.apply(dto.getVoucherCode(), cartTotal);
        if (vr.invalidCode()) {
            result.rejectValue("voucherCode", "voucher.invalid", "Mã voucher không hợp lệ");
        }

        if (result.hasErrors()) {
            enrichCheckoutModel(model, dto);
            return "user/checkout";
        }

        if (cartService.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }

        String email = authentication != null ? authentication.getName() : null;
        Order order;
        try {
            order = orderService.createOrder(dto, email);
        } catch (IllegalArgumentException ex) {
            if ("invalid_voucher".equals(ex.getMessage())) {
                result.rejectValue("voucherCode", "voucher.invalid", "Mã voucher không hợp lệ");
                enrichCheckoutModel(model, dto);
                return "user/checkout";
            }
            throw ex;
        }

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
