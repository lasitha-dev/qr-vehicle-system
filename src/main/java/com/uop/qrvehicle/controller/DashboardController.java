package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.TaskDTO;
import com.uop.qrvehicle.security.CustomUserDetails;
import com.uop.qrvehicle.service.VehicleService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard Controller
 * Displays role-based dashboard with available tasks
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final VehicleService vehicleService;

    public DashboardController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        String username;
        String fullName;
        String userType;

        // Handle both form login and OAuth2 login
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            username = userDetails.getUsername();
            fullName = userDetails.getFullName();
            userType = userDetails.getUserType();
        } else if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            username = oauth2User.getAttribute("email");
            fullName = oauth2User.getAttribute("name");
            userType = "GoogleUser";
        } else {
            username = authentication.getName();
            fullName = authentication.getName();
            userType = "user";
        }

        // Get tasks based on user role
        List<TaskDTO> tasks = getTasksForRole(userType);

        // Add model attributes
        model.addAttribute("username", username);
        model.addAttribute("fullName", fullName);
        model.addAttribute("userType", userType);
        model.addAttribute("tasks", tasks);
        model.addAttribute("pendingCount", vehicleService.getPendingCount());

        return "dashboard/dashboard";
    }

    private List<TaskDTO> getTasksForRole(String userType) {
        List<TaskDTO> tasks = new ArrayList<>();

        tasks.add(new TaskDTO("View / Update Images", "/view/images", "🖼️", "#0275d8"));

        return tasks;
    }
}