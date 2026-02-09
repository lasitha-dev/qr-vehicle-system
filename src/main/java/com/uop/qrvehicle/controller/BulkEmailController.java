package com.uop.qrvehicle.controller;

import com.uop.qrvehicle.model.EmailContact;
import com.uop.qrvehicle.repository.EmailContactRepository;
import com.uop.qrvehicle.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Bulk Email Controller
 * Admin-only endpoint to send batch emails to all contacts in emailtab table.
 * Migrated from: emailstud.php
 */
@Controller
@RequestMapping("/admin/email")
@PreAuthorize("hasRole('ADMIN')")
public class BulkEmailController {

    private static final Logger log = LoggerFactory.getLogger(BulkEmailController.class);

    private final EmailContactRepository emailContactRepository;
    private final EmailService emailService;

    public BulkEmailController(EmailContactRepository emailContactRepository,
                                EmailService emailService) {
        this.emailContactRepository = emailContactRepository;
        this.emailService = emailService;
    }

    /**
     * Show bulk email form and contact list
     */
    @GetMapping
    public String bulkEmailPage(Model model) {
        List<EmailContact> contacts = emailContactRepository.findByEmailIsNotNull();
        model.addAttribute("contacts", contacts);
        model.addAttribute("contactCount", contacts.size());

        // Default subject and body (matching PHP template)
        model.addAttribute("defaultSubject", "UOP Vehicle Registration Confirmation");
        model.addAttribute("defaultBody", buildDefaultEmailBody());

        return "admin/email";
    }

    /**
     * Send bulk emails to all contacts in emailtab
     */
    @PostMapping("/send")
    public String sendBulkEmails(@RequestParam String subject,
                                  @RequestParam String body,
                                  RedirectAttributes redirectAttributes) {
        List<EmailContact> contacts = emailContactRepository.findByEmailIsNotNull();

        if (contacts.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No email contacts found in the database.");
            return "redirect:/admin/email";
        }

        int sent = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();

        for (EmailContact contact : contacts) {
            if (contact.getEmail() == null || contact.getEmail().isEmpty()) {
                continue;
            }

            boolean success = emailService.sendBulkEmail(contact.getEmail(), subject, body);
            if (success) {
                sent++;
            } else {
                failed++;
                errors.append(contact.getEmail()).append(", ");
            }
        }

        String message = String.format("Bulk email complete: %d sent, %d failed out of %d contacts.",
                sent, failed, contacts.size());
        if (failed > 0) {
            message += " Failed: " + errors.toString();
        }

        if (failed == 0) {
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            redirectAttributes.addFlashAttribute("error", message);
        }

        log.info("Bulk email results: {} sent, {} failed", sent, failed);
        return "redirect:/admin/email";
    }

    private String buildDefaultEmailBody() {
        return """
            <b>Link : <u>https://gatepass.pdn.ac.lk</u></b><br>
            User Name: Emp Provident fund no in five digits (Ex: 01234)<br>
            Password: Emp NIC no (As in ETF statement)<br><br>
            Silva WANR - Computer Programmer, Information Technology Center<br>
            Tel : +94 81 239 2470<br>
            """;
    }
}
