package com.fashionshop.controller;

import com.fashionshop.dto.AccountUpdateDTO;
import com.fashionshop.dto.LoginDTO;
import com.fashionshop.dto.RegisterDTO;
import com.fashionshop.entity.User;
import com.fashionshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                           BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        if (userService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.email", "Email đã tồn tại");
            return "auth/register";
        }

        userService.register(dto);
        return "redirect:/login?registerSuccess";
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        model.addAttribute("user", user);
        return "user/account";
    }

    @GetMapping("/account/edit")
    public String editAccount(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        AccountUpdateDTO dto = new AccountUpdateDTO();
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone() != null ? user.getPhone() : "");
        dto.setAddress(user.getAddress() != null ? user.getAddress() : "");

        model.addAttribute("accountUpdateDTO", dto);
        return "user/account-edit";
    }

    @PostMapping("/account/update")
    public String updateAccount(@Valid AccountUpdateDTO dto,
                                BindingResult result,
                                Authentication authentication,
                                Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("accountUpdateDTO", dto);
            return "user/account-edit";
        }

        String email = authentication.getName();
        userService.updateAccount(email, dto);
        return "redirect:/account?updateSuccess";
    }
}
