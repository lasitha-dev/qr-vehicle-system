package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.repository.StaffRepository;
import com.uop.qrvehicle.service.ImageService;
import com.uop.qrvehicle.service.PersonService;
import com.uop.qrvehicle.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Image Management Controller
 * Manages profile images and ID card preview
 * Migrated from: view-update-image.php, view-update-imagemod.php, view-update-imagenow.php
 */
@Controller
@RequestMapping("/view/images")
public class ImageManagementController {

    private static final Logger log = LoggerFactory.getLogger(ImageManagementController.class);

    private final ImageService imageService;
    private final PersonService personService;
    private final StaffRepository staffRepository;
    private final StudentService studentService;

    public ImageManagementController(ImageService imageService,
                                     PersonService personService,
                                     StaffRepository staffRepository,
                                     StudentService studentService) {
        this.imageService = imageService;
        this.personService = personService;
        this.staffRepository = staffRepository;
        this.studentService = studentService;
    }

    /**
     * Image management landing page with category/ID selection
     * Supports cascading: category → (faculty → year) → ID
     * GET /view/images
     * GET /view/images?category=Student&id=AG/23/218
     */
    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    public String imageManagement(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String faculty,
            @RequestParam(required = false) String year,
            Model model) {

        // If we have both category and ID, show the detail view
        if (id != null && !id.trim().isEmpty() && category != null) {
            return showImageDetail(category, id.trim(), model);
        }

        // Otherwise show the selection form
        model.addAttribute("category", category);
        model.addAttribute("faculty", faculty);
        model.addAttribute("year", year);

        // Load faculties/years for student cascading (via AJAX, but provide some defaults)
        if ("Student".equalsIgnoreCase(category)) {
            model.addAttribute("showCascading", true);
        }

        return "view/images";
    }

    /**
     * Show image detail for a specific person
     */
    private String showImageDetail(String category, String id, Model model) {
        model.addAttribute("category", category);
        model.addAttribute("id", id);

        // Find person
        Optional<PersonSearchResult> personOpt = personService.searchPerson(id);
        if (personOpt.isPresent()) {
            model.addAttribute("person", personOpt.get());
        }

        // Get current image URL
        String imageUrl = imageService.getProfileImageUrl(category, id);
        model.addAttribute("currentImageUrl", imageUrl);
        model.addAttribute("hasImage", imageUrl != null);

        // For staff: compute ID card expiry date
        if (!"Student".equalsIgnoreCase(category) && !"Visitor".equalsIgnoreCase(category)) {
            computeIdCardExpiry(id, model);
        }

        // List archived images
        try {
            List<String> archived = imageService.listArchivedImages(id);
            model.addAttribute("archivedImages", archived);
        } catch (Exception e) {
            log.debug("Could not list archived images: {}", e.getMessage());
        }

        model.addAttribute("showDetail", true);
        return "view/images";
    }

    /**
     * Upload a profile image
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('VIEWER')")
    public String uploadImage(@RequestParam String category,
                              @RequestParam String id,
                              @RequestParam("profile_image") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            String url = imageService.uploadProfileImage(file, category, id);
            redirectAttributes.addFlashAttribute("success",
                    "Image uploaded successfully. Old image archived.");
            log.info("Image uploaded for {} ({}): {}", id, category, url);
        } catch (Exception e) {
            log.error("Image upload failed for {} ({})", id, category, e);
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }

        return "redirect:/view/images?category=" + category + "&id=" + id;
    }

    /**
     * Delete a profile image
     */
    @PostMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteImage(@RequestParam String category,
                              @RequestParam String id,
                              RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = imageService.deleteProfileImage(category, id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Image deleted.");
            } else {
                redirectAttributes.addFlashAttribute("error", "No image found to delete.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        return "redirect:/view/images?category=" + category + "&id=" + id;
    }

    /**
     * Compute ID card expiry date:
     * Academic staff → DOB + 65 years
     * Others → DOB + 60 years
     * (mirrors PHP logic in view-update-image.php)
     */
    private void computeIdCardExpiry(String empNo, Model model) {
        try {
            Optional<Staff> staffOpt = staffRepository.findLatestByEmpNo(empNo);
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                model.addAttribute("salaryDate", staff.getSalaryDate());
                model.addAttribute("staffNic", staff.getNic());
                model.addAttribute("staffEmployeeType", staff.getEmployeeType());

                // Parse DOB and compute expiry
                if (staff.getDateOfBirth() != null && !staff.getDateOfBirth().isEmpty()) {
                    try {
                        LocalDate dob = LocalDate.parse(staff.getDateOfBirth(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        int retirementAge = staff.isAcademic() ? 65 : 60;
                        LocalDate expiryDate = dob.plusYears(retirementAge);
                        model.addAttribute("expiryDate",
                                expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                        model.addAttribute("showIdCard", true);
                    } catch (DateTimeParseException e) {
                        log.debug("Could not parse DOB for {}: {}", empNo, staff.getDateOfBirth());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not compute expiry for {}: {}", empNo, e.getMessage());
        }
    }
}
