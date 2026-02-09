package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.service.IdCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * ID Card Controller
 * Generates and serves printable ID cards with front/back backgrounds,
 * passport photo, and text overlay (name, designation, NIC, emp number, expiry).
 * Migrated from: id.php
 */
@Controller
@RequestMapping("/idcard")
@PreAuthorize("hasAnyRole('ADMIN', 'ENTRY', 'VIEWER')")
public class IdCardController {

    private static final Logger log = LoggerFactory.getLogger(IdCardController.class);

    private final IdCardService idCardService;

    public IdCardController(IdCardService idCardService) {
        this.idCardService = idCardService;
    }

    /**
     * ID card preview page
     * GET /idcard/preview?empno=12345
     */
    @GetMapping("/preview")
    public String previewPage(@RequestParam(required = false) String empno, Model model) {
        model.addAttribute("empno", empno);
        if (empno != null && !empno.trim().isEmpty()) {
            model.addAttribute("showCard", true);
        }
        return "idcard/preview";
    }

    /**
     * Serve front side of ID card as PNG image
     * GET /idcard/front/12345.png
     */
    @GetMapping("/front/{empno}.png")
    public ResponseEntity<byte[]> frontCard(@PathVariable String empno) {
        try {
            byte[] imageBytes = idCardService.generateFrontCard(empno);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(imageBytes);
        } catch (IllegalArgumentException e) {
            log.warn("ID card request for unknown staff: {}", empno);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to generate front card for {}: {}", empno, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Serve back side of ID card as PNG image
     * GET /idcard/back/12345.png
     */
    @GetMapping("/back/{empno}.png")
    public ResponseEntity<byte[]> backCard(@PathVariable String empno) {
        try {
            byte[] imageBytes = idCardService.generateBackCard(empno);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("Failed to generate back card for {}: {}", empno, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
