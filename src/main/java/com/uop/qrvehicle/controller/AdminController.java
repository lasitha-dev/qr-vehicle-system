package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Admin Controller
 * Admin-only functions: database backup, system management
 * Migrated from: backup.php
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final BackupService backupService;

    public AdminController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Admin tools page showing backup management
     */
    @GetMapping("/backup")
    public String backupPage(Model model) {
        try {
            model.addAttribute("backups", backupService.listBackups());
        } catch (Exception e) {
            log.error("Failed to list backups: {}", e.getMessage());
            model.addAttribute("error", "Could not list backups: " + e.getMessage());
        }
        return "admin/backup";
    }

    /**
     * Trigger a new database backup
     */
    @PostMapping("/backup/create")
    public String createBackup(RedirectAttributes redirectAttributes) {
        try {
            String backupPath = backupService.createBackup();
            redirectAttributes.addFlashAttribute("success",
                    "Backup created successfully: " + backupPath);
            log.info("Database backup created: {}", backupPath);
        } catch (Exception e) {
            log.error("Backup creation failed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Backup failed: " + e.getMessage());
        }
        return "redirect:/admin/backup";
    }

    /**
     * Download a backup file
     */
    @GetMapping("/backup/download/{filename}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) {
        try {
            Path file = backupService.getBackupFile(filename);
            if (!Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(file))
                    .body(resource);
        } catch (Exception e) {
            log.error("Backup download failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete a backup file
     */
    @PostMapping("/backup/delete")
    public String deleteBackup(@RequestParam String filename,
                               RedirectAttributes redirectAttributes) {
        try {
            if (backupService.deleteBackup(filename)) {
                redirectAttributes.addFlashAttribute("success",
                        "Backup deleted: " + filename);
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Backup not found: " + filename);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/backup";
    }
}
