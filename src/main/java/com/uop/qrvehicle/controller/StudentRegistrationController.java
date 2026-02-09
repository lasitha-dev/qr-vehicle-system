package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.dto.StudentRegistrationDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Student Registration REST API
 * Accepts JSON POST to register new students in studdb.
 * Migrated from: register.php
 */
@RestController
@RequestMapping("/api/students")
public class StudentRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(StudentRegistrationController.class);

    private final JdbcTemplate studDbJdbcTemplate;

    public StudentRegistrationController(@Qualifier("studDbJdbcTemplate") JdbcTemplate studDbJdbcTemplate) {
        this.studDbJdbcTemplate = studDbJdbcTemplate;
    }

    /**
     * Register a new student.
     * POST /api/students/register
     * Body: { "enrolNo": "...", "nameWithInitials": "...", "nic": "...", 
     *         "faculty": "...", "course": "...", "mobile": "...", "email": "...", "address": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerStudent(
            @Valid @RequestBody StudentRegistrationDTO dto) {
        try {
            // Check if student already exists
            Integer count = studDbJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM students WHERE enrol_no = ?",
                    Integer.class, dto.getEnrolNo());

            if (count != null && count > 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Student with enrollment number " + dto.getEnrolNo() + " already exists"
                ));
            }

            // Insert student
            String sql = """
                INSERT INTO students (enrol_no, name_with_initials, nic, faculty, course, mobile, email, address)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            studDbJdbcTemplate.update(sql,
                    dto.getEnrolNo(),
                    dto.getNameWithInitials(),
                    dto.getNic(),
                    dto.getFaculty(),
                    dto.getCourse(),
                    dto.getMobile(),
                    dto.getEmail(),
                    dto.getAddress());

            log.info("Student registered: {} ({})", dto.getEnrolNo(), dto.getNameWithInitials());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Student saved successfully!"
            ));

        } catch (Exception e) {
            log.error("Student registration failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }
}
