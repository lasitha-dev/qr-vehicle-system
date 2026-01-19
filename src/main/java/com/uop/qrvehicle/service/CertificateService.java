package com.uop.qrvehicle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Certificate Service - Handles certificate file uploads and management
 */
@Service
public class CertificateService {

    @Value("${app.upload.certificate-path}")
    private String certificatePath;

    /**
     * Upload a certificate file
     */
    public String uploadCertificate(MultipartFile file, String category, String id, 
                                    String vehicleNo) throws IOException {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Certificate file is required");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        // Build directory path based on category
        String directoryPath = buildDirectoryPath(category, id);
        Path directory = Paths.get(certificatePath, directoryPath);
        Files.createDirectories(directory);

        // Generate filename
        String safeVehicleNo = sanitizeFilename(vehicleNo);
        String safeId = sanitizeFilename(id);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        
        String filename = safeId + "_" + safeVehicleNo + "_" + timestamp + "_" + originalFilename;
        Path filePath = directory.resolve(filename);

        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    /**
     * List all certificates for a specific person
     */
    public List<String> listCertificates(String category, String id) throws IOException {
        List<String> certificates = new ArrayList<>();

        String directoryPath = buildDirectoryPath(category, id);
        Path directory = Paths.get(certificatePath, directoryPath);

        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(Files::isRegularFile)
                     .forEach(path -> certificates.add(path.getFileName().toString()));
            }
        }

        return certificates;
    }

    /**
     * Delete a certificate
     */
    public boolean deleteCertificate(String category, String id, String filename) 
            throws IOException {
        
        String directoryPath = buildDirectoryPath(category, id);
        Path filePath = Paths.get(certificatePath, directoryPath, filename);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        }
        return false;
    }

    /**
     * Get certificate file path
     */
    public Path getCertificatePath(String category, String id, String filename) {
        String directoryPath = buildDirectoryPath(category, id);
        return Paths.get(certificatePath, directoryPath, filename);
    }

    private String buildDirectoryPath(String category, String id) {
        switch (category.toLowerCase()) {
            case "student":
                // Parse student ID: e.g., "AG/23/218" -> "Student/AG/2023"
                String[] parts = id.split("/");
                String prefix = parts.length > 0 ? parts[0] : "S";
                String yearPart = parts.length > 1 ? parts[1] : "00";
                String year = yearPart.length() == 2 ? "20" + yearPart : yearPart;
                return "Student/" + prefix + "/" + year;
                
            case "permanent":
            case "temporary":
            case "casual":
            case "contract":
            case "institute":
                String catName = category.substring(0, 1).toUpperCase() + category.substring(1);
                return "Staff/" + catName + "/" + sanitizeFilename(id);
                
            case "visitor":
                return "Visitor/" + sanitizeFilename(id);
                
            default:
                return "Misc/" + sanitizeFilename(id);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
