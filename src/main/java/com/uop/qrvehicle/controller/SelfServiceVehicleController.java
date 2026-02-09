package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.dto.StudentDetailDTO;
import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.repository.StaffRepository;
import com.uop.qrvehicle.security.CustomUserDetails;
import com.uop.qrvehicle.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Self-Service Vehicle Registration Controller
 * Mirrors PHP insert_vehiclemod.php / insert_vehiclemodmod.php / now insert-vehicle.php
 * 
 * Allows end-users (authenticated via OIDC/Google/form) to:
 * - Auto-detect their category (student vs staff from their user ID)
 * - View their personal info card
 * - See their existing registered vehicles
 * - Register a new vehicle with validation
 * - Receive email confirmation
 */
@Controller
@RequestMapping("/my/vehicle")
public class SelfServiceVehicleController {

    private static final Logger log = LoggerFactory.getLogger(SelfServiceVehicleController.class);

    private final VehicleService vehicleService;
    private final StudentService studentService;
    private final PersonService personService;
    private final CertificateService certificateService;
    private final EmailService emailService;
    private final StaffRepository staffRepository;

    public SelfServiceVehicleController(VehicleService vehicleService,
                                         StudentService studentService,
                                         PersonService personService,
                                         CertificateService certificateService,
                                         EmailService emailService,
                                         StaffRepository staffRepository) {
        this.vehicleService = vehicleService;
        this.studentService = studentService;
        this.personService = personService;
        this.certificateService = certificateService;
        this.emailService = emailService;
        this.staffRepository = staffRepository;
    }

    /**
     * Self-service vehicle registration page.
     * Auto-detects user ID from session (OIDC uid, or form login username).
     * Then auto-detects category (student vs staff) and loads person info.
     * Mirrors PHP insert_vehiclemod.php GET handler.
     */
    @GetMapping
    public String selfServiceForm(Authentication authentication,
                                   HttpServletRequest request,
                                   Model model) {

        String userId = resolveUserId(authentication, request);
        if (userId == null || userId.isEmpty()) {
            model.addAttribute("error", "Unable to determine your user ID. Please contact the administrator.");
            return "vehicle/self-service";
        }

        model.addAttribute("userId", userId);
        model.addAttribute("vehicleTypes", vehicleService.getActiveVehicleTypes());

        // Auto-detect category and load person info
        String category = detectCategory(userId);
        model.addAttribute("category", category);

        if ("student".equals(category)) {
            loadStudentInfo(userId, model);
        } else if ("Permanent".equals(category) || "staff".equals(category)) {
            loadStaffInfo(userId, model);
        } else {
            model.addAttribute("error", "User ID not found or not activated in Student or Staff records.");
            return "vehicle/self-service";
        }

        // Load existing vehicles
        List<Vehicle> existingVehicles = vehicleService.getVehiclesByEmpId(userId);
        model.addAttribute("existingVehicles", existingVehicles);

        return "vehicle/self-service";
    }

    /**
     * Handle self-service vehicle registration form submission.
     * Mirrors PHP insert_vehiclemod.php POST handler.
     */
    @PostMapping("/add")
    public String addVehicle(@RequestParam String userId,
                             @RequestParam String category,
                             @RequestParam String vehicleNo,
                             @RequestParam String owner,
                             @RequestParam(required = false) Integer vehicleTypeId,
                             @RequestParam String mobile,
                             @RequestParam String email,
                             @RequestParam("certificate") MultipartFile certificate,
                             Authentication authentication,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {

        String sessionUser = resolveUserId(authentication, request);

        // Validation — mirrors PHP validation in insert_vehiclemod.php
        vehicleNo = vehicleNo.trim().toUpperCase();

        if (vehicleNo.isEmpty() || owner.isEmpty() || mobile.isEmpty() || email.isEmpty()
                || certificate == null || certificate.isEmpty() || vehicleTypeId == null) {
            redirectAttributes.addFlashAttribute("error",
                "Please fill all details: Vehicle Number, Owner Name, Mobile No, Email, Registration Certificate and Vehicle Type");
            return "redirect:/my/vehicle";
        }

        if (!vehicleNo.matches("^[A-Za-z0-9/\\-\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Invalid vehicle number format.");
            return "redirect:/my/vehicle";
        }

        if (!mobile.matches("^\\+?\\d{7,15}$")) {
            redirectAttributes.addFlashAttribute("error", "Invalid mobile number format.");
            return "redirect:/my/vehicle";
        }

        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            redirectAttributes.addFlashAttribute("error", "Invalid email address.");
            return "redirect:/my/vehicle";
        }

        try {
            // Map category to type
            String type = "student".equalsIgnoreCase(category) ? "Student" : "Permanent";

            // Add vehicle
            vehicleService.addVehicle(userId, vehicleNo, owner, type, vehicleTypeId,
                                       mobile, email, sessionUser != null ? sessionUser : userId);

            // Upload certificate
            certificateService.uploadCertificate(certificate, category, userId, vehicleNo);

            // Send confirmation email
            String personName = owner;
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                baseUrl += ":" + request.getServerPort();
            }
            emailService.sendVehicleRegistrationConfirmation(email, personName, userId,
                                                              vehicleNo, type, baseUrl);

            redirectAttributes.addFlashAttribute("success",
                "Vehicle " + vehicleNo + " registered successfully! A confirmation email has been sent to " + email);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error registering vehicle for user {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/my/vehicle";
    }

    /**
     * Resolve the user ID from the current authentication context.
     * Order of precedence:
     * 1. OIDC uid claim (from Keycloak session)
     * 2. OIDC email
     * 3. Form login username
     */
    private String resolveUserId(Authentication authentication, HttpServletRequest request) {
        if (authentication == null) return null;

        // Check session for OIDC uid (set by KeycloakOidcSuccessHandler)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String oidcUid = (String) session.getAttribute("oidc_uid");
            if (oidcUid != null && !oidcUid.isEmpty()) {
                return oidcUid;
            }
        }

        // Check if OIDC user with uid claim
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String uid = oidcUser.getClaimAsString("uid");
            if (uid != null && !uid.isEmpty()) return uid;
            return oidcUser.getEmail();
        }

        // Form login
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUsername();
        }

        return authentication.getName();
    }

    /**
     * Auto-detect user category based on ID format.
     * Mirrors PHP logic in insert_vehiclemod.php.
     * Student IDs match pattern: X/XX/XXX (e.g., AG/23/218)
     * Staff IDs are numeric employee numbers.
     */
    private String detectCategory(String userId) {
        // Check if student (pattern: LETTERS/DIGITS/...)
        if (userId.matches("^[A-Z]+/\\d{2}/.*")) {
            if (studentService.isRegisteredStudent(userId)) {
                return "student";
            }
        }

        // Check if staff (by querying salary records)
        Optional<Staff> staff = staffRepository.findLatestByEmpNo(userId);
        if (staff.isPresent()) {
            return "Permanent";
        }

        return "unknown";
    }

    /**
     * Load student info into the model.
     */
    private void loadStudentInfo(String regNo, Model model) {
        studentService.getStudentBasicInfo(regNo).ifPresent(student -> {
            model.addAttribute("personName", student.getFullName());
            model.addAttribute("personFaculty", student.getFacultyName());
            model.addAttribute("personCourse", student.getCourseName());
            model.addAttribute("personSemester", student.getSemesterName());
            model.addAttribute("personSemesterRegDate", student.getSemesterRegDate());
            model.addAttribute("personImageUrl", student.getImageUrl());
            model.addAttribute("personAppYear", student.getAppYear());
        });
    }

    /**
     * Load staff info into the model.
     */
    private void loadStaffInfo(String empNo, Model model) {
        staffRepository.findLatestByEmpNo(empNo).ifPresent(staff -> {
            model.addAttribute("personName", staff.getEmpName());
            model.addAttribute("personDesignation", staff.getDesignation());
            model.addAttribute("personDepartment", staff.getDepartment());
            model.addAttribute("personCategory", staff.getCategory());
        });
    }
}
