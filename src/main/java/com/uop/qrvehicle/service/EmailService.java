package com.uop.qrvehicle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email Service
 * Sends vehicle registration confirmation and notification emails.
 * Mirrors PHP PHPMailer usage in insert_vehiclemod.php and emailstud.php.
 * Uses Spring Boot's JavaMailSender with SMTP (Gmail) configuration.
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

    /**
     * Send vehicle registration confirmation email.
     * Mirrors PHP insert_vehiclemod.php email sending logic.
     *
     * @param toEmail    Recipient email address
     * @param personName Person's full name
     * @param personId   Employee/Student ID
     * @param vehicleNo  Registered vehicle number
     * @param category   Person category (Student, Permanent, etc.)
     * @param baseUrl    Application base URL for certificate download link
     */
    @Async
    public void sendVehicleRegistrationConfirmation(String toEmail, String personName,
                                                     String personId, String vehicleNo,
                                                     String category, String baseUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("UOP Vehicle Registration Confirmation - " + vehicleNo);

            String htmlBody = buildRegistrationConfirmationHtml(personName, personId, 
                                                                vehicleNo, category, baseUrl);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Registration confirmation email sent to {} for vehicle {}", toEmail, vehicleNo);

        } catch (MessagingException e) {
            log.error("Failed to send registration email to {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Send vehicle approval notification email.
     */
    @Async
    public void sendApprovalNotification(String toEmail, String personName,
                                          String vehicleNo, String status) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Vehicle " + status + " - " + vehicleNo);

            String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #800000; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h2>University of Peradeniya</h2>
                        <p>Vehicle Registration Update</p>
                    </div>
                    <div style="padding: 20px; border: 1px solid #ddd; border-top: none;">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Your vehicle <strong>%s</strong> has been <strong style="color: %s;">%s</strong>.</p>
                        %s
                        <hr>
                        <p style="color: #666; font-size: 12px;">
                            Silva WANR - Computer Programmer, Information Technology Center<br>
                            Tel: +94 81 239 2470
                        </p>
                    </div>
                </div>
                """.formatted(
                    personName,
                    vehicleNo,
                    "Approved".equals(status) ? "#28a745" : "#dc3545",
                    status,
                    "Approved".equals(status) 
                        ? "<p>Your vehicle is now authorized for campus entry.</p>"
                        : "<p>Please contact the administration for more information.</p>"
                );

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Approval notification email sent to {} for vehicle {} ({})", toEmail, vehicleNo, status);

        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Build HTML body for registration confirmation email.
     * Mirrors PHP email template from insert_vehiclemod.php.
     */
    private String buildRegistrationConfirmationHtml(String personName, String personId,
                                                      String vehicleNo, String category,
                                                      String baseUrl) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: #800000; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h2>University of Peradeniya</h2>
                    <p>Vehicle Registration Confirmation</p>
                </div>
                <div style="padding: 20px; border: 1px solid #ddd; border-top: none;">
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
                    <p style="color: #666; font-size: 12px;">
                        Silva WANR - Computer Programmer, Information Technology Center<br>
                        Tel: +94 81 239 2470
                    </p>
                </div>
            </div>
            """.formatted(personName, personId, category, vehicleNo, baseUrl, personId);
    }

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
}
