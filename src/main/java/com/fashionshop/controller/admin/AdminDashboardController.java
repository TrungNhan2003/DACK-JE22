package com.fashionshop.controller.admin;

import com.fashionshop.service.OrderService;
import com.fashionshop.service.ProductService;
import com.fashionshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("todayOrders", orderService.countTodayOrders());
        model.addAttribute("revenueThisMonth", orderService.revenueThisMonth());
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalUsers", userService.findAll().size());
        model.addAttribute("recentOrders", orderService.findTop10Latest());
        model.addAttribute("lowStockProducts", productService.findLowStock());
        return "admin/dashboard";
    }
}
