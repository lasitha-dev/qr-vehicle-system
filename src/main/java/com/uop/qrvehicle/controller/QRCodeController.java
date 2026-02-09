package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.model.Visitor;
import com.uop.qrvehicle.repository.StaffRepository;
import com.uop.qrvehicle.repository.TemporaryStaffRepository;
import com.uop.qrvehicle.repository.VisitorRepository;
import com.uop.qrvehicle.service.QRCodeService;
import com.uop.qrvehicle.service.StudentService;
import com.google.zxing.WriterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * QR Code Controller
 * Handles QR code generation for vehicles
 * Migrated from: generate_qr.php
 */
@Controller
@RequestMapping("/qr")
public class QRCodeController {

    private static final Logger log = LoggerFactory.getLogger(QRCodeController.class);

    private final QRCodeService qrCodeService;
    private final StudentService studentService;
    private final StaffRepository staffRepository;
    private final TemporaryStaffRepository temporaryStaffRepository;
    private final VisitorRepository visitorRepository;

    @Value("${app.upload.qrcode-path}")
    private String qrCodePath;

    public QRCodeController(QRCodeService qrCodeService,
                           StudentService studentService,
                           StaffRepository staffRepository,
                           TemporaryStaffRepository temporaryStaffRepository,
                           VisitorRepository visitorRepository) {
        this.qrCodeService = qrCodeService;
        this.studentService = studentService;
        this.staffRepository = staffRepository;
        this.temporaryStaffRepository = temporaryStaffRepository;
        this.visitorRepository = visitorRepository;
    }

    /**
     * QR code generation form
     */
    @GetMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateForm(Model model) {
        return "qr/generate";
    }

    /**
     * Generate QR code for display
     */
    @GetMapping("/code")
    public ResponseEntity<byte[]> generateQRCode(@RequestParam String content) {
        try {
            byte[] qrImage = qrCodeService.generateQRCode(content);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            
            return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
        } catch (WriterException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate and save QR codes for all registered students (batch)
     * Mirrors generate_qr.php student logic:
     *   - Queries all registered students from studdb
     *   - Generates QR under qrcodes/Student/<Faculty>/<Year>/
     *   - Skips if QR already exists
     */
    @PostMapping("/generate/student")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateStudentQRCodes(RedirectAttributes redirectAttributes,
                                         jakarta.servlet.http.HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort();

            // Get all registered students from studdb
            List<com.uop.qrvehicle.dto.StudentDetailDTO> students = studentService.searchStudents("");

            int generated = 0;
            int skipped = 0;

            for (com.uop.qrvehicle.dto.StudentDetailDTO student : students) {
                String regNo = student.getRegNo();
                if (regNo == null || regNo.isEmpty()) continue;

                // Parse reg number to build path: e.g., "AG/23/218" → Student/AG/2023
                String[] parts = regNo.split("/");
                String faculty = parts.length > 0 ? parts[0] : "UNKNOWN";
                String year = parts.length > 1 ? "20" + parts[1] : "0000";

                // Check if QR already exists
                String filename = regNo.replace("/", "_") + ".png";
                Path qrPath = Paths.get(qrCodePath, "Student", faculty, year, filename);

                if (Files.exists(qrPath)) {
                    skipped++;
                    continue;
                }

                // Generate QR code
                String content = baseUrl + "/search/person?id=" + regNo;
                qrCodeService.generateAndSaveQRCode(content, "Student/" + faculty + "/" + year, regNo);
                generated++;
            }

            redirectAttributes.addFlashAttribute("success",
                    "Student QR codes: " + generated + " generated, " + skipped + " already existed.");
            log.info("Batch student QR generation complete: {} generated, {} skipped", generated, skipped);

        } catch (Exception e) {
            log.error("Error generating student QR codes", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/qr/generate";
    }

    /**
     * Generate and save QR codes for staff (batch)
     * Mirrors generate_qr.php staff logic:
     *   - Permanent: queries slipspaymentsdetailall (latest record per employee)
     *   - Temporary/Casual/Contract/Institute: queries temporarystaff
     *   - Visitor: queries visitor table
     *   - Saves under qrcodes/Staff/<Type>/ or qrcodes/Visitor/
     */
    @PostMapping("/generate/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateStaffQRCodes(@RequestParam String staffType,
                                       RedirectAttributes redirectAttributes,
                                       jakarta.servlet.http.HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort();

            int generated = 0;
            int skipped = 0;

            if ("Visitor".equalsIgnoreCase(staffType)) {
                // Generate for visitors
                List<Visitor> visitors = visitorRepository.findAllByOrderByIdDesc();
                for (Visitor v : visitors) {
                    String filename = v.getId().replace("/", "_") + ".png";
                    Path qrPath = Paths.get(qrCodePath, "Visitor", filename);

                    if (Files.exists(qrPath)) {
                        skipped++;
                        continue;
                    }

                    String content = baseUrl + "/search/person?id=" + v.getId();
                    qrCodeService.generateAndSaveQRCode(content, "Visitor", v.getId());
                    generated++;
                }
            } else if ("Permanent".equalsIgnoreCase(staffType)) {
                // Get latest record per permanent employee
                List<Staff> staffList = staffRepository.findAllLatestRecords();
                for (Staff s : staffList) {
                    String filename = s.getEmpNo().replace("/", "_") + ".png";
                    Path qrPath = Paths.get(qrCodePath, "Staff", "Permanent", filename);

                    if (Files.exists(qrPath)) {
                        skipped++;
                        continue;
                    }

                    String content = baseUrl + "/search/person?id=" + s.getEmpNo();
                    qrCodeService.generateAndSaveQRCode(content, "Staff/Permanent", s.getEmpNo());
                    generated++;
                }
            } else {
                // Temporary, Casual, Contract, Institute
                var tempList = temporaryStaffRepository.findByCategory(staffType);
                for (var t : tempList) {
                    String filename = t.getEmpNo().replace("/", "_") + ".png";
                    Path qrPath = Paths.get(qrCodePath, "Staff", staffType, filename);

                    if (Files.exists(qrPath)) {
                        skipped++;
                        continue;
                    }

                    String content = baseUrl + "/search/person?id=" + t.getEmpNo();
                    qrCodeService.generateAndSaveQRCode(content, "Staff/" + staffType, t.getEmpNo());
                    generated++;
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    staffType + " QR codes: " + generated + " generated, " + skipped + " already existed.");
            log.info("Batch {} QR generation complete: {} generated, {} skipped",
                    staffType, generated, skipped);

        } catch (Exception e) {
            log.error("Error generating {} QR codes", staffType, e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/qr/generate";
    }
}
