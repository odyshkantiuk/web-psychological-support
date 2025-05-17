package com.psychsupport.webpsychologicalsupport.controller.web;

import com.psychsupport.webpsychologicalsupport.dto.UserDto;
import com.psychsupport.webpsychologicalsupport.dto.UserRegistrationDto;
import com.psychsupport.webpsychologicalsupport.model.User;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(userDto);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(principal.getName());
        
        if (user.getRole() == User.Role.ADMIN) {
            return "redirect:/admin/dashboard";
        } else if (user.getRole() == User.Role.PSYCHOLOGIST) {
            return "redirect:/psychologist/dashboard";
        } else {
            return "redirect:/client/dashboard";
        }
    }
}