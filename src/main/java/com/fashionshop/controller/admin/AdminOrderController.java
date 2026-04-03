package com.fashionshop.controller.admin;

import com.fashionshop.entity.Order;
import com.fashionshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String orders(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id).orElse(null));
        return "admin/order-detail";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, Order.OrderStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra!");
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, Order.OrderStatus.CANCELLED);
            redirectAttributes.addFlashAttribute("success", "Hủy đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra!");
        }
        return "redirect:/admin/orders";
    }
}
