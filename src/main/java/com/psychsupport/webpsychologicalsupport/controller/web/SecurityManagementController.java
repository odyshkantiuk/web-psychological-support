package com.psychsupport.webpsychologicalsupport.controller.web;

import com.psychsupport.webpsychologicalsupport.security.LoginAttemptService;
import com.psychsupport.webpsychologicalsupport.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/security")
@RequiredArgsConstructor
public class SecurityManagementController {

    private final LoginAttemptService loginAttemptService;
    private final UserService userService;

    @GetMapping("/blocked-users")
    public String viewBlockedUsers(Model model) {
        Map<String, Integer> blockedUsers = loginAttemptService.getBlockedUsers();
        model.addAttribute("blockedUsers", blockedUsers);
        model.addAttribute("maxAttempts", loginAttemptService.getMaxAttempts());
        model.addAttribute("blockDuration", loginAttemptService.getBlockDurationMinutes());
        
        return "admin/blocked-users";
    }
    
    @PostMapping("/unblock/{username}")
    public String unblockUser(@PathVariable String username, RedirectAttributes redirectAttributes) {
        loginAttemptService.resetAttempts(username);
        redirectAttributes.addFlashAttribute("success", "User '" + username + "' has been unblocked successfully.");
        return "redirect:/admin/security/blocked-users";
    }
    
    @PostMapping("/unblock-all")
    public String unblockAllUsers(RedirectAttributes redirectAttributes) {
        Map<String, Integer> blockedUsers = loginAttemptService.getBlockedUsers();
        
        for (String username : blockedUsers.keySet()) {
            loginAttemptService.resetAttempts(username);
        }
        
        redirectAttributes.addFlashAttribute("success", blockedUsers.size() + " users have been unblocked successfully.");
        return "redirect:/admin/security/blocked-users";
    }
}