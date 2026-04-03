package com.fashionshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "redirect:/";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "redirect:/access-denied";
    }
}
