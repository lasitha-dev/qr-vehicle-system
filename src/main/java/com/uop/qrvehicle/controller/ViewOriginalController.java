package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.PersonSearchResult;
import com.uop.qrvehicle.model.Vehicle;
import com.uop.qrvehicle.repository.VehicleRepository;
import com.uop.qrvehicle.service.PersonService;
import com.uop.qrvehicle.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * View Original Controller
 * Combined person + vehicle detail dashboard
 * Migrated from: view-original.php
 */
@Controller
@RequestMapping("/view")
public class ViewOriginalController {

    private static final Logger log = LoggerFactory.getLogger(ViewOriginalController.class);

    private final PersonService personService;
    private final VehicleRepository vehicleRepository;
    private final StudentService studentService;

    public ViewOriginalController(PersonService personService,
                                  VehicleRepository vehicleRepository,
                                  StudentService studentService) {
        this.personService = personService;
        this.vehicleRepository = vehicleRepository;
        this.studentService = studentService;
    }

    /**
     * Combined person + vehicle detail page
     * GET /view/detail?id=XXX&vehicleno=YYY&category=Student|Staff|...
     */
    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRY', 'VIEWER')")
    public String viewDetail(@RequestParam String id,
                             @RequestParam(required = false) String vehicleno,
                             @RequestParam(required = false, defaultValue = "auto") String category,
                             Model model) {

        log.debug("View detail: id={}, vehicleno={}, category={}", id, vehicleno, category);

        // Find person
        Optional<PersonSearchResult> personOpt = personService.searchPerson(id);
        if (personOpt.isEmpty()) {
            model.addAttribute("error", "Person not found: " + id);
            return "view/detail";
        }

        PersonSearchResult person = personOpt.get();
        model.addAttribute("person", person);

        // If a specific vehicle was requested, load it
        if (vehicleno != null && !vehicleno.trim().isEmpty()) {
            vehicleRepository.findByEmpIdAndVehicleNo(id, vehicleno)
                    .ifPresent(v -> model.addAttribute("selectedVehicle", v));
        }

        // Load semester info for students
        if ("Student".equalsIgnoreCase(person.getType())) {
            try {
                studentService.getStudentDetail(id).ifPresent(detail -> {
                    model.addAttribute("semesterInfo", detail.getSemesterName());
                    model.addAttribute("studentDetail", detail);
                });
            } catch (Exception e) {
                log.debug("Could not load student detail for view-original: {}", e.getMessage());
            }
        }

        return "view/detail";
    }
}
