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

        if (userType == null) userType = "user";
        String role = userType.toLowerCase().replace(" ", "");

        switch (role) {
            case "admin":
                tasks.add(new TaskDTO("Insert New Vehicle", "/vehicle/insert", "🚗", "#f0ad4e"));
                tasks.add(new TaskDTO("QR Generator", "/qr/generate", "📝", "#5cb85c"));
                tasks.add(new TaskDTO("Student Details", "/student/detail", "🎓", "#17a2b8"));
                tasks.add(new TaskDTO("Staff Details", "/staff/detail", "👔", "#6c757d"));
                tasks.add(new TaskDTO("View/Update Images", "/view/images", "👁️", "#0275d8"));
                tasks.add(new TaskDTO("Search Vehicle", "/vehicle/search", "🔍", "#6f42c1"));
                tasks.add(new TaskDTO("Person Search", "/search/person", "🧑‍💼", "#ff851b"));
                tasks.add(new TaskDTO("Pending Approvals", "/vehicle/pending", "⏳", "#d9534f"));
                tasks.add(new TaskDTO("Plate Scanner", "/vehicle/scanner", "📷", "#795548"));
                tasks.add(new TaskDTO("ID Card Preview", "/idcard/preview", "🪪", "#37474f"));
                tasks.add(new TaskDTO("DB Backup", "/admin/backup", "🗄️", "#263238"));
                tasks.add(new TaskDTO("Bulk Email", "/admin/email", "📧", "#1565c0"));
                break;
                
            case "entry":
                tasks.add(new TaskDTO("Insert New Vehicle", "/vehicle/insert", "🚗", "#f0ad4e"));
                tasks.add(new TaskDTO("Student Details", "/student/detail", "🎓", "#17a2b8"));
                tasks.add(new TaskDTO("Staff Details", "/staff/detail", "👔", "#6c757d"));
                tasks.add(new TaskDTO("Search Vehicle", "/vehicle/search", "🔍", "#6f42c1"));
                tasks.add(new TaskDTO("Person Search", "/search/person", "🧑‍💼", "#ff851b"));
                tasks.add(new TaskDTO("Plate Scanner", "/vehicle/scanner", "📷", "#795548"));
                tasks.add(new TaskDTO("ID Card Preview", "/idcard/preview", "🪪", "#37474f"));
                break;
                
            case "viewer":
                tasks.add(new TaskDTO("Student Details", "/student/detail", "🎓", "#17a2b8"));
                tasks.add(new TaskDTO("Staff Details", "/staff/detail", "👔", "#6c757d"));
                tasks.add(new TaskDTO("View/Update Images", "/view/images", "👁️", "#0275d8"));
                tasks.add(new TaskDTO("ID Card Preview", "/idcard/preview", "🪪", "#37474f"));
                break;
                
            case "searcher":
                tasks.add(new TaskDTO("Search Vehicle", "/vehicle/search", "🔍", "#6f42c1"));
                break;
                
            case "googleuser":
            default:
                // Self-service vehicle registration for OIDC/Google users
                tasks.add(new TaskDTO("Register My Vehicle", "/my/vehicle", "🚗", "#f0ad4e"));
                tasks.add(new TaskDTO("Person Search", "/search/person", "🧑‍💼", "#ff851b"));
                break;
        }

        return tasks;
    }
}