package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.StaffDetailDTO;
import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.repository.VehicleRepository;
import com.uop.qrvehicle.service.CertificateService;
import com.uop.qrvehicle.service.StaffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * Staff Controller
 * Staff detail profile viewer and search
 * Migrated from: view_staff.php, search_person.php (staff sections)
 */
@Controller
@RequestMapping("/staff")
public class StaffController {

    private static final Logger log = LoggerFactory.getLogger(StaffController.class);

    private final StaffService staffService;
    private final VehicleRepository vehicleRepository;
    private final CertificateService certificateService;

    public StaffController(StaffService staffService,
                          VehicleRepository vehicleRepository,
                          CertificateService certificateService) {
        this.staffService = staffService;
        this.vehicleRepository = vehicleRepository;
        this.certificateService = certificateService;
    }

    /**
     * Staff detail page
     * GET /staff/detail?empno=12345
     */
    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY', 'VIEWER')")
    public String staffDetail(@RequestParam(required = false) String empno, Model model) {

        if (empno == null || empno.trim().isEmpty()) {
            // Show search form only
            return "staff/detail";
        }

        empno = empno.trim();

        Optional<StaffDetailDTO> staffOpt = staffService.getStaffDetail(empno);
        if (staffOpt.isEmpty()) {
            model.addAttribute("error", "Staff member not found: " + empno);
            model.addAttribute("searchEmpNo", empno);
            return "staff/detail";
        }

        StaffDetailDTO staff = staffOpt.get();
        model.addAttribute("staff", staff);

        // Load vehicles
        List<Vehicle> vehicles = vehicleRepository.findByEmpIdOrderByCreateDateDesc(empno);
        model.addAttribute("vehicles", vehicles);

        // Load certificates
        try {
            String certCategory = staff.getCategory() != null ? staff.getCategory() : "Staff";
            List<String> certificates = certificateService.listCertificates(certCategory, empno);
            model.addAttribute("certificates", certificates);
        } catch (Exception e) {
            log.debug("Could not load certificates for {}: {}", empno, e.getMessage());
        }

        return "staff/detail";
    }

    /**
     * Staff search
     * GET /staff/search?query=XXX
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY', 'VIEWER')")
    public String staffSearch(@RequestParam(required = false) String query, Model model) {

        if (query != null && !query.trim().isEmpty()) {
            List<StaffDetailDTO> results = staffService.searchStaff(query.trim());
            model.addAttribute("results", results);
            model.addAttribute("query", query.trim());
        }

        return "staff/search";
    }
}
