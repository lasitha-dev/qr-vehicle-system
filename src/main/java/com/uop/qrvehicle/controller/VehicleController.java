package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.security.CustomUserDetails;
import com.uop.qrvehicle.service.CertificateService;
import com.uop.qrvehicle.service.EmailService;
import com.uop.qrvehicle.service.StudentService;
import com.uop.qrvehicle.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Vehicle Controller
 * Handles vehicle registration, search, and management.
 * Admin actions (approve/reject/update/delete/certificate-delete) now trigger
 * email notifications to the vehicle owner (and student master email when applicable).
 */
@Controller
@RequestMapping("/vehicle")
public class VehicleController {

    private static final Logger log = LoggerFactory.getLogger(VehicleController.class);

    private final VehicleService vehicleService;
    private final CertificateService certificateService;
    private final EmailService emailService;
    private final StudentService studentService;

    public VehicleController(VehicleService vehicleService,
                            CertificateService certificateService,
                            EmailService emailService,
                            StudentService studentService) {
        this.vehicleService = vehicleService;
        this.certificateService = certificateService;
        this.emailService = emailService;
        this.studentService = studentService;
    }

    /**
     * Vehicle insert form
     */
    @GetMapping("/insert")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY')")
    public String insertForm(@RequestParam(required = false) String category,
                            @RequestParam(required = false) String id,
                            @RequestParam(required = false) String status,
                            Model model) {
        try {
            log.debug("insertForm called: category={}, id={}, status={}", category, id, status);
            
            model.addAttribute("category", category);
            model.addAttribute("selectedId", id);
            model.addAttribute("selectedStatus", status);
            
            // Add vehicle types for dropdown
            model.addAttribute("vehicleTypes", vehicleService.getActiveVehicleTypes());
            
            if (id != null && !id.isEmpty()) {
                List<Vehicle> vehicles;
                if (status != null && !status.isBlank()) {
                    vehicles = vehicleService.getVehiclesByEmpIdAndStatus(id, status);
                } else {
                    vehicles = vehicleService.getVehiclesByEmpId(id);
                }
                model.addAttribute("vehicles", vehicles);
                log.debug("Found {} vehicles for id={}, status={}", vehicles.size(), id, status);

                // For students, load certificate-to-vehicle map for PDF display
                if ("student".equalsIgnoreCase(category) && !vehicles.isEmpty()) {
                    Map<String, List<String>> vehicleCertMap = certificateService.getCertificatesByVehicleMap(
                            "student", id, vehicles);
                    model.addAttribute("vehicleCertMap", vehicleCertMap);

                    // Compute cert base URL path: Student/{prefix}/{year}
                    String[] regParts = id.split("/");
                    String prefix = regParts.length > 0 ? regParts[0] : "S";
                    String yearPart = regParts.length > 1 ? regParts[1] : "00";
                    String year = yearPart.length() == 2 ? "20" + yearPart : yearPart;
                    model.addAttribute("certBasePath", "/uploads/certificates/Student/" + prefix + "/" + year);
                }
            }
            
            return "vehicle/insert";
        } catch (Exception e) {
            log.error("Error in insertForm: category={}, id={}, status={}", category, id, status, e);
            throw e;
        }
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
                            @RequestParam(required = false) Integer vehicleTypeId,
                            @RequestParam(required = false) String mobile,
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) String status,
                            @RequestParam("certificate") MultipartFile certificate,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = getUsername(authentication);
            String type = mapCategoryToType(category);
            
            // Add vehicle with new fields
            vehicleService.addVehicle(id, vehicleNo, owner, type, vehicleTypeId, mobile, email, username);
            
            // Upload certificate if provided
            if (certificate != null && !certificate.isEmpty()) {
                certificateService.uploadCertificate(certificate, category, id, vehicleNo);
            }
            
            redirectAttributes.addFlashAttribute("success", "Vehicle added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        String redirectUrl = "redirect:/vehicle/insert?category=" + category + "&id=" + id;
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return redirectUrl;
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
                               @RequestParam(required = false) Integer vehicleTypeId,
                               @RequestParam(required = false) String mobile,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String status,
                               @RequestParam(value = "certificate", required = false) MultipartFile certificate,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = getUsername(authentication);
            
            // Update vehicle
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, oldVehicleNo, vehicleNo, owner, 
                                        approvalStatus, vehicleTypeId, mobile, email, username);
            
            // Rename existing certificate files if vehicle number changed
            if (!oldVehicleNo.equals(vehicleNo)) {
                try {
                    certificateService.renameCertificatesForVehicle(category, id, oldVehicleNo, vehicleNo);
                } catch (Exception renameEx) {
                    log.warn("Failed to rename certificates for vehicle {} -> {}: {}", 
                             oldVehicleNo, vehicleNo, renameEx.getMessage());
                }
            }

            // Upload new certificate if provided
            if (certificate != null && !certificate.isEmpty()) {
                certificateService.uploadCertificate(certificate, category, id, vehicleNo);
            }

            // Send update notification email
            List<String> recipients = emailService.resolveRecipientEmails(updatedVehicle, studentService);
            if (!recipients.isEmpty()) {
                String certUrl = buildCertificateUrl(category, id);
                boolean emailSent = emailService.sendVehicleUpdatedNotification(
                        recipients, updatedVehicle.getOwner(), vehicleNo,
                        updatedVehicle.getType(), updatedVehicle.getMobile(),
                        updatedVehicle.getApprovalStatus(), certUrl);
                redirectAttributes.addFlashAttribute("success",
                        "Vehicle updated successfully!" + (emailSent 
                            ? " Notification sent to " + String.join(", ", recipients) + "."
                            : " (Email notification failed)"));
            } else {
                redirectAttributes.addFlashAttribute("success",
                        "Vehicle updated successfully! (No email on file)");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        String redirectUrl = "redirect:/vehicle/insert?category=" + category + "&id=" + id;
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return redirectUrl;
    }

    /**
     * Delete vehicle
     */
    @PostMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteVehicle(@RequestParam String category,
                               @RequestParam String id,
                               @RequestParam String vehicleNo,
                               @RequestParam(required = false) String status,
                               RedirectAttributes redirectAttributes) {
        try {
            // Fetch vehicle before deletion to capture owner/email/type for notification
            Optional<Vehicle> vehicleOpt = vehicleService.getVehicle(id, vehicleNo);
            
            vehicleService.deleteVehicle(id, vehicleNo);

            // Send deletion notification email
            if (vehicleOpt.isPresent()) {
                Vehicle vehicle = vehicleOpt.get();
                List<String> recipients = emailService.resolveRecipientEmails(vehicle, studentService);
                if (!recipients.isEmpty()) {
                    boolean emailSent = emailService.sendVehicleDeletedNotification(
                            recipients, vehicle.getOwner(), vehicleNo, vehicle.getType());
                    redirectAttributes.addFlashAttribute("success",
                            "Vehicle deleted successfully!" + (emailSent
                                ? " Notification sent to " + String.join(", ", recipients) + "."
                                : " (Email notification failed)"));
                } else {
                    redirectAttributes.addFlashAttribute("success",
                            "Vehicle deleted successfully! (No email on file)");
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "Vehicle deleted successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        String redirectUrl = "redirect:/vehicle/insert?category=" + category + "&id=" + id;
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return redirectUrl;
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
     * Vehicle number plate OCR scanner page.
     * Uses Tesseract.js client-side OCR.
     * Migrated from: App now.php, app1.php
     */
    @GetMapping("/scanner")
    public String vehicleScanner() {
        return "vehicle/scanner";
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
     * Approve vehicle using composite key (empId and vehicleNo)
     */
    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveVehicle(@RequestParam String empId, 
                                @RequestParam String vehicleNo,
                                RedirectAttributes redirectAttributes) {
        try {
            Vehicle vehicle = vehicleService.approveVehicle(empId, vehicleNo);

            // Send approval notification email
            List<String> recipients = emailService.resolveRecipientEmails(vehicle, studentService);
            if (!recipients.isEmpty()) {
                boolean emailSent = emailService.sendApprovalNotification(
                        recipients, vehicle.getOwner(), vehicleNo,
                        vehicle.getType(), vehicle.getMobile(), "Approved");
                redirectAttributes.addFlashAttribute("success",
                        "Vehicle approved!" + (emailSent
                            ? " Notification sent to " + String.join(", ", recipients) + "."
                            : " (Email notification failed)"));
            } else {
                redirectAttributes.addFlashAttribute("success", "Vehicle approved! (No email on file)");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/vehicle/pending";
    }

    /**
     * Reject vehicle using composite key (empId and vehicleNo)
     */
    @PostMapping("/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectVehicle(@RequestParam String empId,
                               @RequestParam String vehicleNo,
                               RedirectAttributes redirectAttributes) {
        try {
            Vehicle vehicle = vehicleService.rejectVehicle(empId, vehicleNo);

            // Send rejection notification email
            List<String> recipients = emailService.resolveRecipientEmails(vehicle, studentService);
            if (!recipients.isEmpty()) {
                boolean emailSent = emailService.sendApprovalNotification(
                        recipients, vehicle.getOwner(), vehicleNo,
                        vehicle.getType(), vehicle.getMobile(), "Rejected");
                redirectAttributes.addFlashAttribute("success",
                        "Vehicle rejected!" + (emailSent
                            ? " Notification sent to " + String.join(", ", recipients) + "."
                            : " (Email notification failed)"));
            } else {
                redirectAttributes.addFlashAttribute("success", "Vehicle rejected! (No email on file)");
            }
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

    /**
     * Delete a certificate file.
     * Migrated from: delete_certificate.php
     */
    @PostMapping("/certificate/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCertificate(@RequestParam String category,
                                     @RequestParam String id,
                                     @RequestParam String filename,
                                     @RequestParam(required = false) String returnUrl,
                                     @RequestParam(required = false) String status,
                                     RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = certificateService.deleteCertificate(category, id, filename);
            if (deleted) {
                // Send certificate deleted notification email
                // Extract vehicle number from filename or find from existing vehicles
                sendCertificateDeletedEmail(category, id, filename, redirectAttributes);
                // Only set success if not already set by the email helper
                if (!redirectAttributes.getFlashAttributes().containsKey("success")) {
                    redirectAttributes.addFlashAttribute("success", "Certificate deleted: " + filename);
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Certificate not found: " + filename);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        String redirectUrl = "redirect:/vehicle/insert?category=" + category + "&id=" + id;
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return redirectUrl;
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

    /**
     * Build the certificate download base URL for a given category and person ID.
     */
    private String buildCertificateUrl(String category, String id) {
        if ("student".equalsIgnoreCase(category) && id != null) {
            String[] regParts = id.split("/");
            String prefix = regParts.length > 0 ? regParts[0] : "S";
            String yearPart = regParts.length > 1 ? regParts[1] : "00";
            String year = yearPart.length() == 2 ? "20" + yearPart : yearPart;
            return "/uploads/certificates/Student/" + prefix + "/" + year;
        }
        return "/uploads/certificates/" + (category != null ? category : "Other");
    }

    /**
     * Find the vehicle associated with a certificate filename and send notification.
     * Looks up all vehicles for the given person ID and tries to match
     * the filename to a vehicle number. Falls back to using first vehicle if needed.
     */
    private void sendCertificateDeletedEmail(String category, String id, String filename,
                                              RedirectAttributes redirectAttributes) {
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesByEmpId(id);
            if (vehicles.isEmpty()) {
                redirectAttributes.addFlashAttribute("success",
                        "Certificate deleted: " + filename + " (No vehicle found for notification)");
                return;
            }

            // Try to match the filename to a vehicle number
            // Certificate naming convention: vehicleNo_timestamp.ext or similar
            Vehicle matchedVehicle = null;
            for (Vehicle v : vehicles) {
                if (filename.contains(v.getVehicleNo().replace("/", "_"))
                        || filename.contains(v.getVehicleNo())) {
                    matchedVehicle = v;
                    break;
                }
            }
            // Fallback to first vehicle if no filename match
            if (matchedVehicle == null) {
                matchedVehicle = vehicles.get(0);
            }

            List<String> recipients = emailService.resolveRecipientEmails(matchedVehicle, studentService);
            if (!recipients.isEmpty()) {
                boolean emailSent = emailService.sendCertificateDeletedNotification(
                        recipients, matchedVehicle.getOwner(), matchedVehicle.getVehicleNo(),
                        matchedVehicle.getType(), filename);
                redirectAttributes.addFlashAttribute("success",
                        "Certificate deleted: " + filename + (emailSent
                            ? " Notification sent to " + String.join(", ", recipients) + "."
                            : " (Email notification failed)"));
            } else {
                redirectAttributes.addFlashAttribute("success",
                        "Certificate deleted: " + filename + " (No email on file)");
            }
        } catch (Exception e) {
            log.warn("Failed to send certificate deleted notification for {}: {}", filename, e.getMessage());
            redirectAttributes.addFlashAttribute("success", "Certificate deleted: " + filename);
        }
    }
}
