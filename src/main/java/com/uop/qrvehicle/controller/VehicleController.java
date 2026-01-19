package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.security.CustomUserDetails;
import com.uop.qrvehicle.service.CertificateService;
import com.uop.qrvehicle.service.VehicleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Vehicle Controller
 * Handles vehicle registration, search, and management
 */
@Controller
@RequestMapping("/vehicle")
public class VehicleController {

    private final VehicleService vehicleService;
    private final CertificateService certificateService;

    public VehicleController(VehicleService vehicleService, 
                            CertificateService certificateService) {
        this.vehicleService = vehicleService;
        this.certificateService = certificateService;
    }

    /**
     * Vehicle insert form
     */
    @GetMapping("/insert")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY')")
    public String insertForm(@RequestParam(required = false) String category,
                            @RequestParam(required = false) String id,
                            Model model) {
        
        model.addAttribute("category", category);
        model.addAttribute("selectedId", id);
        
        if (id != null && !id.isEmpty()) {
            List<Vehicle> vehicles = vehicleService.getVehiclesByEmpId(id);
            model.addAttribute("vehicles", vehicles);
        }
        
        return "vehicle/insert";
    }

    /**
     * Add new vehicle
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY')")
    public String addVehicle(@RequestParam String category,
                            @RequestParam String id,
                            @RequestParam String vehicleNo,
                            @RequestParam(required = false) String owner,
                            @RequestParam("certificate") MultipartFile certificate,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = getUsername(authentication);
            String type = mapCategoryToType(category);
            
            // Add vehicle
            vehicleService.addVehicle(id, vehicleNo, owner, type, username);
            
            // Upload certificate if provided
            if (certificate != null && !certificate.isEmpty()) {
                certificateService.uploadCertificate(certificate, category, id, vehicleNo);
            }
            
            redirectAttributes.addFlashAttribute("success", "Vehicle added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/vehicle/insert?category=" + category + "&id=" + id;
    }

    /**
     * Update vehicle
     */
    @PostMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateVehicle(@RequestParam String category,
                               @RequestParam String id,
                               @RequestParam String oldVehicleNo,
                               @RequestParam String vehicleNo,
                               @RequestParam(required = false) String owner,
                               @RequestParam(required = false) String approvalStatus,
                               @RequestParam(value = "certificate", required = false) MultipartFile certificate,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = getUsername(authentication);
            
            // Update vehicle
            vehicleService.updateVehicle(id, oldVehicleNo, vehicleNo, owner, 
                                        approvalStatus, username);
            
            // Upload new certificate if provided
            if (certificate != null && !certificate.isEmpty()) {
                certificateService.uploadCertificate(certificate, category, id, vehicleNo);
            }
            
            redirectAttributes.addFlashAttribute("success", "Vehicle updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/vehicle/insert?category=" + category + "&id=" + id;
    }

    /**
     * Search vehicles
     */
    @GetMapping("/search")
    public String searchVehicles(@RequestParam(required = false) String query, Model model) {
        if (query != null && !query.isEmpty()) {
            List<Vehicle> results = vehicleService.searchByVehicleNo(query);
            model.addAttribute("vehicles", results);
            model.addAttribute("query", query);
        }
        return "vehicle/search";
    }

    /**
     * Pending vehicles for approval
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public String pendingVehicles(Model model) {
        List<Vehicle> pendingVehicles = vehicleService.getPendingVehicles();
        model.addAttribute("vehicles", pendingVehicles);
        return "vehicle/pending";
    }

    /**
     * Approve vehicle
     */
    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveVehicle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.approveVehicle(id);
            redirectAttributes.addFlashAttribute("success", "Vehicle approved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/vehicle/pending";
    }

    /**
     * Reject vehicle
     */
    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectVehicle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.rejectVehicle(id);
            redirectAttributes.addFlashAttribute("success", "Vehicle rejected!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/vehicle/pending";
    }

    private String getUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUsername();
        }
        return authentication.getName();
    }

    private String mapCategoryToType(String category) {
        if (category == null) return "Unknown";
        switch (category.toLowerCase()) {
            case "student": return "Student";
            case "permanent": return "Permanent";
            case "temporary": return "Temporary";
            case "casual": return "Casual";
            case "contract": return "Contract";
            case "institute": return "Institute";
            case "visitor": return "Visitor";
            default: return category;
        }
    }
}
