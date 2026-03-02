package com.uop.qrvehicle.service;

import com.uop.qrvehicle.model.Vehicle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.util.*;

/**
 * Email Service
 * Sends vehicle registration confirmation and notification emails.
 * Mirrors PHP PHPMailer usage in insert_vehicle.php and emailstud.php.
 * Uses Spring Boot's JavaMailSender with SMTP (Gmail) configuration.
 *
 * All vehicle notification methods are synchronous and return boolean
 * so the caller (controller) can report success/failure to the admin.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:nilupas79@gs.pdn.ac.lk}")
    private String fromEmail;

    @Value("${app.mail.from-name:UOP Vehicle Registration}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================================================================
    // Recipient Resolution
    // =========================================================================

    /**
     * Resolve recipient email addresses for a vehicle notification.
     * Uses only the email from the vehicle registration record.
     *
     * @param vehicle        The vehicle entity
     * @param studentService StudentService (unused, kept for API compatibility)
     * @return list containing the vehicle's email, or empty if none on file
     */
    public List<String> resolveRecipientEmails(Vehicle vehicle, StudentService studentService) {
        List<String> emails = new ArrayList<>();

        if (vehicle.getEmail() != null && !vehicle.getEmail().isBlank()) {
            emails.add(vehicle.getEmail().trim());
        }

        return emails;
    }

    // =========================================================================
    // Vehicle Registration Confirmation (self-service flow)
    // =========================================================================

    /**
     * Send vehicle registration confirmation email.
     * Mirrors PHP insert_vehicle.php email sending logic for "Vehicle Added".
     *
     * @return true if the email was sent successfully
     */
    public boolean sendVehicleRegistrationConfirmation(String toEmail, String personName,
                                                        String personId, String vehicleNo,
                                                        String category, String baseUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Vehicle Registered \u2013 " + vehicleNo);

            String htmlBody = buildRegistrationConfirmationHtml(personName, personId,
                                                                 vehicleNo, category, baseUrl);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Registration confirmation email sent to {} for vehicle {}", toEmail, vehicleNo);
            return true;

        } catch (Exception e) {
            log.error("Failed to send registration email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    // =========================================================================
    // Approval / Rejection Notification
    // =========================================================================

    /**
     * Send vehicle approval or rejection notification email to multiple recipients.
     * Mirrors PHP insert_vehicle.php approval status change email behaviour.
     *
     * @param toEmails   List of recipient email addresses (deduplicated)
     * @param personName Vehicle owner name
     * @param vehicleNo  Vehicle number
     * @param category   Person category (Student, Permanent, etc.)
     * @param mobile     Owner mobile number (may be null)
     * @param status     "Approved" or "Rejected"
     * @return true if at least one email was sent successfully
     */
    public boolean sendApprovalNotification(List<String> toEmails, String personName,
                                             String vehicleNo, String category,
                                             String mobile, String status) {
        if (toEmails == null || toEmails.isEmpty()) {
            log.warn("No recipient emails for approval notification of vehicle {}", vehicleNo);
            return false;
        }

        String statusColor = "Approved".equals(status) ? "#28a745" : "#dc3545";
        String actionMessage = "Approved".equals(status)
                ? "<p style=\"color:#28a745;font-weight:bold;\">Your vehicle is now authorized for campus entry.</p>"
                : "<p style=\"color:#dc3545;font-weight:bold;\">Your vehicle registration has been rejected. "
                  + "Please contact the administration for more information.</p>";

        String htmlBody = buildVehicleNotificationHtml(category, vehicleNo, personName,
                mobile, status, null, "Vehicle " + status, actionMessage);

        String subject = "Vehicle " + status + " \u2013 " + vehicleNo;

        return sendToMultipleRecipients(toEmails, subject, htmlBody, "approval/" + status);
    }

    // =========================================================================
    // Vehicle Updated Notification
    // =========================================================================

    /**
     * Send vehicle updated notification email to multiple recipients.
     * Mirrors PHP insert_vehicle.php "Vehicle Updated" email.
     *
     * @return true if at least one email was sent successfully
     */
    public boolean sendVehicleUpdatedNotification(List<String> toEmails, String personName,
                                                   String vehicleNo, String category,
                                                   String mobile, String approvalStatus,
                                                   String certUrl) {
        if (toEmails == null || toEmails.isEmpty()) {
            log.warn("No recipient emails for update notification of vehicle {}", vehicleNo);
            return false;
        }

        String actionMessage = "<p>Your vehicle details have been <strong>updated</strong> by an administrator. "
                + "Please review the information above and contact the relevant office if anything is incorrect.</p>";

        String htmlBody = buildVehicleNotificationHtml(category, vehicleNo, personName,
                mobile, approvalStatus, certUrl, "Vehicle Updated", actionMessage);

        String subject = "Vehicle Updated \u2013 " + vehicleNo;

        return sendToMultipleRecipients(toEmails, subject, htmlBody, "update");
    }

    // =========================================================================
    // Vehicle Deleted Notification
    // =========================================================================

    /**
     * Send vehicle deleted notification email to multiple recipients.
     *
     * @return true if at least one email was sent successfully
     */
    public boolean sendVehicleDeletedNotification(List<String> toEmails, String personName,
                                                   String vehicleNo, String category) {
        if (toEmails == null || toEmails.isEmpty()) {
            log.warn("No recipient emails for delete notification of vehicle {}", vehicleNo);
            return false;
        }

        String actionMessage = "<p style=\"color:#dc3545;font-weight:bold;\">Your vehicle <strong>"
                + vehicleNo + "</strong> has been <strong>removed</strong> from the University Vehicle Gate Pass System.</p>"
                + "<p>If you believe this was done in error, please contact the administration immediately.</p>";

        String htmlBody = buildVehicleNotificationHtml(category, vehicleNo, personName,
                null, "Deleted", null, "Vehicle Deleted", actionMessage);

        String subject = "Vehicle Deleted \u2013 " + vehicleNo;

        return sendToMultipleRecipients(toEmails, subject, htmlBody, "delete");
    }

    // =========================================================================
    // Certificate Deleted Notification
    // =========================================================================

    /**
     * Send certificate deleted notification email to multiple recipients.
     * Mirrors PHP insert_vehicle.php "Certificate Deleted" email.
     *
     * @return true if at least one email was sent successfully
     */
    public boolean sendCertificateDeletedNotification(List<String> toEmails, String personName,
                                                       String vehicleNo, String category,
                                                       String filename) {
        if (toEmails == null || toEmails.isEmpty()) {
            log.warn("No recipient emails for certificate delete notification of vehicle {}", vehicleNo);
            return false;
        }

        String actionMessage = "<p>A registration certificate (<strong>" + filename
                + "</strong>) associated with your vehicle <strong>" + vehicleNo
                + "</strong> has been <strong>deleted</strong> by an administrator.</p>"
                + "<p>If a new certificate is required, please upload it through the system or contact the administration.</p>";

        String htmlBody = buildVehicleNotificationHtml(category, vehicleNo, personName,
                null, null, null, "Certificate Deleted", actionMessage);

        String subject = "Certificate Deleted: " + vehicleNo;

        return sendToMultipleRecipients(toEmails, subject, htmlBody, "certificate-delete");
    }

    // =========================================================================
    // Bulk Email (unchanged)
    // =========================================================================

    /**
     * Send a bulk email to a specific recipient.
     * Used by the batch email sender (migrated from emailstud.php).
     *
     * @param toEmail   Recipient email
     * @param subject   Email subject
     * @param htmlBody  HTML email body
     * @return true if sent successfully
     */
    public boolean sendBulkEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Bulk email sent to: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Bulk email failed for {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Send the same HTML email to multiple recipients.
     * Returns true if at least one send succeeded.
     */
    private boolean sendToMultipleRecipients(List<String> toEmails, String subject,
                                              String htmlBody, String notificationType) {
        boolean anySent = false;
        for (String recipient : toEmails) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, fromName);
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.info("{} notification email sent to {}", notificationType, recipient);
                anySent = true;
            } catch (Exception e) {
                log.error("Failed to send {} notification to {}: {}", notificationType, recipient, e.getMessage(), e);
            }
        }
        return anySent;
    }

    /**
     * Build the shared vehicle notification HTML body.
     * Mirrors PHP buildVehicleEmailBody() from insert_vehicle.php.
     * Includes: UoP header, vehicle details table, action-specific message,
     * category-specific contact sections, and automated footer.
     *
     * @param category       Person category (Student, Permanent, etc.)
     * @param vehicleNo      Vehicle number plate
     * @param owner          Owner name
     * @param mobile         Mobile number (nullable)
     * @param approvalStatus Current approval status (nullable)
     * @param certUrl        Certificate download URL (nullable)
     * @param headerTitle    Title shown in the maroon header (e.g. "Vehicle Approved")
     * @param actionMessage  Action-specific HTML paragraph(s)
     */
    private String buildVehicleNotificationHtml(String category, String vehicleNo,
                                                 String owner, String mobile,
                                                 String approvalStatus, String certUrl,
                                                 String headerTitle, String actionMessage) {

        String salutation = "Student".equalsIgnoreCase(category) ? "Student" : "Staff Member";

        // Build vehicle details rows
        StringBuilder details = new StringBuilder();
        details.append("<p>");
        details.append("<strong>Vehicle Number:</strong> ").append(esc(vehicleNo)).append("<br>");
        details.append("<strong>Owner:</strong> ").append(esc(owner)).append("<br>");
        if (mobile != null && !mobile.isBlank()) {
            details.append("<strong>Mobile:</strong> ").append(esc(mobile)).append("<br>");
        }
        if (approvalStatus != null && !approvalStatus.isBlank()) {
            String statusColor = switch (approvalStatus) {
                case "Approved" -> "#28a745";
                case "Rejected" -> "#dc3545";
                case "Pending" -> "#f0ad4e";
                default -> "#333";
            };
            details.append("<strong>Approval Status:</strong> <span style=\"color:")
                   .append(statusColor).append(";font-weight:bold;\">")
                   .append(esc(approvalStatus)).append("</span><br>");
        }
        details.append("</p>");

        // Certificate link (if provided)
        String certSection = "";
        if (certUrl != null && !certUrl.isBlank()) {
            certSection = "<p><a href='" + esc(certUrl) + "' target='_blank'>"
                    + "\uD83D\uDCC4 Download Registration Certificate</a></p>";
        }

        // Category-specific contact section (mirrors PHP insert_vehicle.php)
        String contactSection = buildCategoryContactSection(category);

        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #800000; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h2 style="margin: 0;">University of Peradeniya</h2>
                    <p style="margin: 5px 0 0 0;">%s</p>
                </div>
                <div style="padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 8px 8px;">
                    <p>Dear %s,</p>
                    %s
                    %s
                    <p>
                        This email is to inform you of an update related to your above vehicle details
                        in the <strong>University of Peradeniya Vehicle Gate Pass System
                        (<a href="https://gatepass.pdn.ac.lk" target="_blank" style="color:#1a73e8;text-decoration:none;">
                            gatepass.pdn.ac.lk
                        </a>)</strong>.
                    </p>
                    %s
                    %s
                    <hr style="margin-top: 20px;">
                    <p style="font-size:12px;color:#555;">
                        This is an automated system-generated email. Please do not reply.
                    </p>
                </div>
            </div>
            """.formatted(
                esc(headerTitle),
                salutation,
                details.toString(),
                certSection,
                actionMessage,
                contactSection
        );
    }

    /**
     * Build category-specific contact information section.
     * Mirrors the PHP buildVehicleEmailBody() category branches.
     */
    private String buildCategoryContactSection(String category) {
        if ("Student".equalsIgnoreCase(category)) {
            return """
                <p>
                    For inquiries related to your <strong>student master / registration details</strong>
                    (excluding vehicle details), please contact the
                    <strong>Student Registration Division</strong>.
                </p>
                <p>
                    \uD83C\uDF10 <a href="https://sites.google.com/gs.pdn.ac.lk/stdregdiv" target="_blank">
                    https://sites.google.com/gs.pdn.ac.lk/stdregdiv</a><br>
                    \uD83D\uDCDE Tel: +94 81 239 2324<br>
                    \u2709 Email: student.registration@gs.pdn.ac.lk
                </p>
                <p>
                    <strong>Student Services Division</strong><br>
                    \uD83C\uDF10 <a href="https://sites.google.com/gs.pdn.ac.lk/stdserdiv" target="_blank">
                    https://sites.google.com/gs.pdn.ac.lk/stdserdiv</a><br>
                    \u2709 drsspdn@gmail.com
                </p>
                """;
        } else {
            return """
                <p><strong>Academic / Academic Support Staff:</strong><br>
                    \uD83C\uDF10 <a href="https://sites.google.com/gs.pdn.ac.lk/acdaffdiv/" target="_blank">
                    Academic Affairs Division</a><br>
                    \uD83C\uDF10 <a href="https://sites.google.com/gs.pdn.ac.lk/acdestdiv" target="_blank">
                    Academic Establishments Division</a>
                </p>
                <p><strong>Non-Academic Staff:</strong><br>
                    \uD83C\uDF10 <a href="https://sites.google.com/gs.pdn.ac.lk/nonacdestdiv" target="_blank">
                    Non-Academic Establishments Division</a><br>
                    \uD83D\uDCDE Direct: 081 239 2441 / 2443<br>
                    \uD83D\uDCDE Ext: 2439 / 2440<br>
                    \u2709 Office: nacest@gs.pdn.ac.lk
                </p>
                <p>
                    <strong>General Services Division</strong><br>
                    \uD83C\uDF10 <a href="https://sites.google.com/gs.pdn.ac.lk/genserdiv" target="_blank">
                    https://sites.google.com/gs.pdn.ac.lk/genserdiv</a><br>
                    \u2709 generalservice@gs.pdn.ac.lk
                </p>
                """;
        }
    }

    /**
     * Build HTML body for registration confirmation email (self-service flow).
     * Mirrors PHP email template from insert_vehicle.php "Vehicle Added".
     */
    private String buildRegistrationConfirmationHtml(String personName, String personId,
                                                      String vehicleNo, String category,
                                                      String baseUrl) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #800000; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h2 style="margin: 0;">University of Peradeniya</h2>
                    <p style="margin: 5px 0 0 0;">Vehicle Registration Confirmation</p>
                </div>
                <div style="padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 8px 8px;">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your vehicle has been successfully registered in the University Vehicle Pass System.</p>
                    <table style="width: 100%%; border-collapse: collapse; margin: 15px 0;">
                        <tr style="border-bottom: 1px solid #eee;">
                            <td style="padding: 8px; font-weight: bold; width: 150px;">ID</td>
                            <td style="padding: 8px;">%s</td>
                        </tr>
                        <tr style="border-bottom: 1px solid #eee;">
                            <td style="padding: 8px; font-weight: bold;">Category</td>
                            <td style="padding: 8px;">%s</td>
                        </tr>
                        <tr style="border-bottom: 1px solid #eee;">
                            <td style="padding: 8px; font-weight: bold;">Vehicle No</td>
                            <td style="padding: 8px;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; font-weight: bold;">Status</td>
                            <td style="padding: 8px;"><span style="color: #f0ad4e; font-weight: bold;">Pending Approval</span></td>
                        </tr>
                    </table>
                    <p>Your registration is pending administrative approval. You will be notified once approved.</p>
                    <p style="margin-top: 20px;">
                        <a href="%s/search/person?id=%s"
                           style="background: #800000; color: white; padding: 10px 25px; text-decoration: none; border-radius: 5px;">
                            View Your Profile
                        </a>
                    </p>
                    <hr style="margin-top: 30px;">
                    <p style="font-size:12px;color:#555;">
                        This is an automated system-generated email. Please do not reply.
                    </p>
                </div>
            </div>
            """.formatted(personName, personId, category, vehicleNo, baseUrl, personId);
    }

    /** Simple HTML-escape for user-supplied values in email bodies. */
    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                     .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
