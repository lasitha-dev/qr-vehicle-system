package com.uop.qrvehicle.service;

import com.uop.qrvehicle.model.Staff;
import com.uop.qrvehicle.model.TemporaryStaff;
import com.uop.qrvehicle.repository.StaffRepository;
import com.uop.qrvehicle.repository.TemporaryStaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * ID Card Rendering Service
 * Generates printable ID cards with front/back backgrounds, passport-size photo,
 * and overlaid text (name, designation, NIC, emp number, expiry date).
 * Migrated from: id.php
 */
@Service
public class IdCardService {

    private static final Logger log = LoggerFactory.getLogger(IdCardService.class);

    // Card dimensions matching PHP (1190 x 754 pixels)
    private static final int CARD_WIDTH = 1190;
    private static final int CARD_HEIGHT = 754;

    // Photo position and size (matches PHP CSS: top:50, left:20, 288x376)
    private static final int PHOTO_X = 20;
    private static final int PHOTO_Y = 50;
    private static final int PHOTO_WIDTH = 288;
    private static final int PHOTO_HEIGHT = 376;

    // Text positions (matches PHP CSS)
    private static final int TEXT_X = 420;
    private static final int NAME_Y = 180;
    private static final int DESG_Y = 230;
    private static final int NIC_Y = 280;
    private static final int EMP_Y = 330;
    private static final int EXP_LABEL_X = 420;
    private static final int EXP_VALUE_X = 560;
    private static final int EXP_Y = 380;

    private final StaffRepository staffRepository;
    private final TemporaryStaffRepository temporaryStaffRepository;
    private final ImageService imageService;

    @Value("${app.upload.image-path}")
    private String imagePath;

    @Value("${app.idcard.front-bg:images/Front.jpg}")
    private String frontBgPath;

    @Value("${app.idcard.back-bg:images/Back.jpg}")
    private String backBgPath;

    public IdCardService(StaffRepository staffRepository,
                         TemporaryStaffRepository temporaryStaffRepository,
                         ImageService imageService) {
        this.staffRepository = staffRepository;
        this.temporaryStaffRepository = temporaryStaffRepository;
        this.imageService = imageService;
    }

    /**
     * Generate front side of ID card as PNG image bytes.
     */
    public byte[] generateFrontCard(String empNo) throws IOException {
        // Look up staff details
        StaffInfo info = getStaffInfo(empNo);
        if (info == null) {
            throw new IllegalArgumentException("Staff member not found: " + empNo);
        }

        BufferedImage card = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        BufferedImage bg = loadBackground(frontBgPath);
        if (bg != null) {
            g.drawImage(bg, 0, 0, CARD_WIDTH, CARD_HEIGHT, null);
        } else {
            // Fallback: white background with UoP maroon header
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);
            g.setColor(new Color(128, 0, 0));
            g.fillRect(0, 0, CARD_WIDTH, 40);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("UNIVERSITY OF PERADENIYA", 400, 28);
        }

        // Draw passport photo
        drawPassportPhoto(g, empNo, info.category);

        // Draw text fields
        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(info.name != null ? info.name : "", TEXT_X, NAME_Y);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString(info.designation != null ? info.designation : "", TEXT_X, DESG_Y);
        g.drawString("NIC: " + (info.nic != null ? info.nic : ""), TEXT_X, NIC_Y);
        g.drawString("Emp No: " + empNo, TEXT_X, EMP_Y);

        // Expiry date
        if (info.expiryDate != null) {
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("Expiry:", EXP_LABEL_X, EXP_Y);
            g.drawString(info.expiryDate, EXP_VALUE_X, EXP_Y);
        }

        g.dispose();

        return toBytes(card);
    }

    /**
     * Generate back side of ID card as PNG image bytes.
     */
    public byte[] generateBackCard(String empNo) throws IOException {
        StaffInfo info = getStaffInfo(empNo);

        BufferedImage card = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background
        BufferedImage bg = loadBackground(backBgPath);
        if (bg != null) {
            g.drawImage(bg, 0, 0, CARD_WIDTH, CARD_HEIGHT, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);
        }

        // Signature line
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String signLabel = "Authorized Signature";
        int signX = (CARD_WIDTH - g.getFontMetrics().stringWidth(signLabel)) / 2;
        g.drawString(signLabel, signX, 240);

        // Draw a line for signature
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(350, 200, 840, 200);

        // Important notice
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String notice = "This card is the property of the University of Peradeniya. " +
                "If found, please return to the Security Office.";
        drawWrappedText(g, notice, 60, 380, 1060, 20);

        g.dispose();

        return toBytes(card);
    }

    /**
     * Look up staff information from both permanent and temporary tables
     */
    private StaffInfo getStaffInfo(String empNo) {
        // Try permanent staff first
        Optional<Staff> staffOpt = staffRepository.findLatestByEmpNo(empNo);
        if (staffOpt.isPresent()) {
            Staff s = staffOpt.get();
            StaffInfo info = new StaffInfo();
            info.name = s.getEmpName();
            info.designation = s.getDesignation();
            info.nic = s.getNic();
            info.category = getCategoryFolder(s.getEmployeeType());
            info.dateOfBirth = s.getDateOfBirth();
            info.isAcademic = s.isAcademic();
            info.expiryDate = computeExpiry(s.getDateOfBirth(), s.isAcademic());
            return info;
        }

        // Try temporary staff
        Optional<TemporaryStaff> tempOpt = temporaryStaffRepository.findByEmpNo(empNo);
        if (tempOpt.isPresent()) {
            TemporaryStaff t = tempOpt.get();
            StaffInfo info = new StaffInfo();
            info.name = t.getEmpName();
            info.designation = t.getDesignation();
            info.nic = t.getNic();
            info.category = t.getCategory() != null ? t.getCategory() : "Staff";
            info.dateOfBirth = t.getDateOfBirth();
            info.isAcademic = false;
            info.expiryDate = computeExpiry(t.getDateOfBirth(), false);
            return info;
        }

        return null;
    }

    private String getCategoryFolder(String employeeType) {
        if (employeeType == null) return "Staff";
        if (employeeType.toLowerCase().contains("academic")) return "Permanent";
        return "Staff";
    }

    private String computeExpiry(String dob, boolean isAcademic) {
        if (dob == null || dob.isEmpty()) return null;
        try {
            LocalDate birthDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int retirementAge = isAcademic ? 65 : 60;
            LocalDate expiry = birthDate.plusYears(retirementAge);
            return expiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.debug("Could not parse DOB: {}", dob);
            return null;
        }
    }

    private void drawPassportPhoto(Graphics2D g, String empNo, String category) {
        // Draw photo frame
        g.setColor(Color.WHITE);
        g.fillRect(PHOTO_X, PHOTO_Y, PHOTO_WIDTH, PHOTO_HEIGHT);

        // Try to load actual photo
        BufferedImage photo = loadStaffPhoto(empNo, category);
        if (photo != null) {
            g.drawImage(photo, PHOTO_X + 3, PHOTO_Y + 3,
                    PHOTO_WIDTH - 6, PHOTO_HEIGHT - 6, null);
        } else {
            // Placeholder
            g.setColor(new Color(240, 240, 240));
            g.fillRect(PHOTO_X + 3, PHOTO_Y + 3, PHOTO_WIDTH - 6, PHOTO_HEIGHT - 6);
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString("No Photo", PHOTO_X + 90, PHOTO_Y + 190);
        }

        // Photo border
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(PHOTO_X, PHOTO_Y, PHOTO_WIDTH, PHOTO_HEIGHT);
    }

    private BufferedImage loadStaffPhoto(String empNo, String category) {
        // Try category-specific path first, then generic Staff path
        String[] categories = {category, "Staff", "Permanent", "Temporary"};
        for (String cat : categories) {
            try {
                Path photoPath = Paths.get(imagePath, cat, empNo + ".jpg");
                if (Files.exists(photoPath)) {
                    return ImageIO.read(photoPath.toFile());
                }
                // Try .png
                photoPath = Paths.get(imagePath, cat, empNo + ".png");
                if (Files.exists(photoPath)) {
                    return ImageIO.read(photoPath.toFile());
                }
            } catch (IOException e) {
                log.debug("Could not load photo for {} in {}", empNo, cat);
            }
        }
        return null;
    }

    private BufferedImage loadBackground(String path) {
        try {
            // Try as classpath resource first
            InputStream is = getClass().getClassLoader().getResourceAsStream("static/" + path);
            if (is != null) {
                return ImageIO.read(is);
            }
            // Try as file path
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                return ImageIO.read(filePath.toFile());
            }
        } catch (IOException e) {
            log.debug("Could not load background: {}", path);
        }
        return null;
    }

    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;

        for (String word : words) {
            String testLine = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(testLine) > maxWidth) {
                g.drawString(line.toString(), x, currentY);
                line = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, currentY);
        }
    }

    private byte[] toBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static class StaffInfo {
        String name;
        String designation;
        String nic;
        String category;
        String dateOfBirth;
        boolean isAcademic;
        String expiryDate;
    }
}
