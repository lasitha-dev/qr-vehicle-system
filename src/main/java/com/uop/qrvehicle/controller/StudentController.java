package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.StudentDetailDTO;
import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.service.CertificateService;
import com.uop.qrvehicle.service.StudentService;
import com.uop.qrvehicle.service.VehicleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Student Controller
 * Provides comprehensive student detail view matching PHP view.php.
 * Shows registration info, personal details, address, contact, A/L results,
 * parent/guardian info, semester history, and vehicle registrations.
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final VehicleService vehicleService;
    private final CertificateService certificateService;

    public StudentController(StudentService studentService,
                            VehicleService vehicleService,
                            CertificateService certificateService) {
        this.studentService = studentService;
        this.vehicleService = vehicleService;
        this.certificateService = certificateService;
    }

    /**
     * Student detail view
     * Mirrors PHP view.php — comprehensive student profile page.
     */
    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY', 'VIEWER')")
    public String studentDetail(@RequestParam(required = false) String regno, Model model) {
        if (regno == null || regno.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a student registration number.");
            return "student/detail";
        }

        Optional<StudentDetailDTO> studentOpt = studentService.getStudentDetail(regno.trim());

        if (studentOpt.isEmpty()) {
            model.addAttribute("error", "Student not found: " + regno);
            model.addAttribute("searchRegNo", regno);
            return "student/detail";
        }

        StudentDetailDTO student = studentOpt.get();
        model.addAttribute("student", student);
        model.addAttribute("searchRegNo", regno);

        // Get vehicles for this student
        List<Vehicle> vehicles = vehicleService.getVehiclesByEmpId(regno.trim());
        model.addAttribute("vehicles", vehicles);

        // Get certificates
        try {
            List<String> certificates = certificateService.listCertificates("student", regno.trim());
            model.addAttribute("certificates", certificates);
        } catch (IOException e) {
            model.addAttribute("certificates", Collections.emptyList());
        }

        return "student/detail";
    }

    /**
     * Student search — find students by registration number or name.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY', 'VIEWER')")
    public String searchStudents(@RequestParam(required = false) String query, Model model) {
        if (query != null && !query.trim().isEmpty()) {
            List<StudentDetailDTO> results = studentService.searchStudents(query.trim());
            model.addAttribute("students", results);
            model.addAttribute("query", query);
        }
        return "student/search";
    }
}
