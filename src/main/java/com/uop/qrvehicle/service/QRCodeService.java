package com.uop.qrvehicle.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * QR Code Service - Generates QR codes for vehicles
 */
@Service
public class QRCodeService {

    @Value("${app.upload.qrcode-path}")
    private String qrCodePath;

    private static final int QR_WIDTH = 250;
    private static final int QR_HEIGHT = 250;

    /**
     * Generate QR code and return as byte array
     */
    public byte[] generateQRCode(String content) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 
                                                   QR_WIDTH, QR_HEIGHT, hints);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        
        return baos.toByteArray();
    }

    /**
     * Generate QR code and save to file
     */
    public String generateAndSaveQRCode(String content, String category, String id) 
            throws WriterException, IOException {
        
        byte[] qrBytes = generateQRCode(content);
        
        // Create directory structure
        String categoryPath = buildCategoryPath(category, id);
        Path directory = Paths.get(qrCodePath, categoryPath);
        Files.createDirectories(directory);
        
        // Generate filename
        String filename = sanitizeFilename(id) + ".png";
        Path filePath = directory.resolve(filename);
        
        // Save file
        Files.write(filePath, qrBytes);
        
        return filePath.toString();
    }

    /**
     * Generate QR code for a student
     */
    public String generateStudentQRCode(String regNo, String baseUrl) 
            throws WriterException, IOException {
        
        String content = baseUrl + "/search/person?id=" + regNo;
        
        // Parse registration number: e.g., "AG/23/218"
        String[] parts = regNo.split("/");
        String prefix = parts.length > 0 ? parts[0] : "UNKNOWN";
        String year = parts.length > 1 ? "20" + parts[1] : "0000";
        
        String categoryPath = "Student/" + prefix + "/" + year;
        
        return generateAndSaveQRCode(content, categoryPath, regNo);
    }

    /**
     * Generate QR code for staff
     */
    public String generateStaffQRCode(String empNo, String staffType, String baseUrl) 
            throws WriterException, IOException {
        
        String content = baseUrl + "/search/person?id=" + empNo;
        String categoryPath = "Staff/" + staffType + "/" + empNo;
        
        return generateAndSaveQRCode(content, categoryPath, empNo);
    }

    /**
     * Generate QR code for visitor
     */
    public String generateVisitorQRCode(Long visitorId, String baseUrl) 
            throws WriterException, IOException {
        
        String content = baseUrl + "/search/person?id=VIS_" + visitorId;
        String categoryPath = "Visitor";
        
        return generateAndSaveQRCode(content, categoryPath, "VIS_" + visitorId);
    }

    private String buildCategoryPath(String category, String id) {
        return category.replace("\\", "/");
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
