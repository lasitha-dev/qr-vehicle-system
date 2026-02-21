package com.uop.qrvehicle.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Authentication Controller
 * Handles login page and authentication-related views
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        
        if (error != null) {
            if ("domain".equals(error)) {
                model.addAttribute("error", "Access denied: Only @pdn.ac.lk emails are allowed");
            } else if ("oauth".equals(error)) {
                model.addAttribute("error", "Google authentication failed. Please try again.");
            } else {
                model.addAttribute("error", "Invalid username or password");
            }
        }
        
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        
        return "auth/login";
    }

    @org.springframework.web.bind.annotation.RequestMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
