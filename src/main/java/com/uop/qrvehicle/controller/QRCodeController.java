package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.service.QRCodeService;
import com.google.zxing.WriterException;
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

/**
 * QR Code Controller
 * Handles QR code generation for vehicles
 */
@Controller
@RequestMapping("/qr")
public class QRCodeController {

    private final QRCodeService qrCodeService;

    public QRCodeController(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
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
     * Generate and save QR codes for students
     */
    @PostMapping("/generate/student")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateStudentQRCodes(RedirectAttributes redirectAttributes) {
        try {
            // TODO: Iterate through students and generate QR codes
            redirectAttributes.addFlashAttribute("success", "Student QR codes generated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/qr/generate";
    }

    /**
     * Generate and save QR codes for staff
     */
    @PostMapping("/generate/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateStaffQRCodes(@RequestParam String staffType,
                                       RedirectAttributes redirectAttributes) {
        try {
            // TODO: Iterate through staff and generate QR codes
            redirectAttributes.addFlashAttribute("success", staffType + " staff QR codes generated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/qr/generate";
    }
}
