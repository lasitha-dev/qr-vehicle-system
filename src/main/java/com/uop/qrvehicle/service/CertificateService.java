package com.uop.qrvehicle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.uop.qrvehicle.model.Vehicle;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
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

    /**
     * List certificates for a specific vehicle.
     * Matches by checking if the certificate filename contains the sanitized vehicle number.
     * Filename convention: {safeId}_{safeVehicleNo}_{timestamp}_{originalFilename}
     */
    public List<String> listCertificatesForVehicle(String category, String id, String vehicleNo) throws IOException {
        String safeVehicleNo = sanitizeFilename(vehicleNo);
        return listCertificates(category, id).stream()
                .filter(cert -> cert.contains(safeVehicleNo))
                .collect(Collectors.toList());
    }

    /**
     * Build a map of vehicle number -> list of certificate filenames.
     * Used by the student profile page to display PDFs matched to each vehicle.
     */
    public Map<String, List<String>> getCertificatesByVehicleMap(String category, String id, List<Vehicle> vehicles) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        try {
            List<String> allCerts = listCertificates(category, id);
            for (Vehicle v : vehicles) {
                String safeVehicleNo = sanitizeFilename(v.getVehicleNo());
                List<String> matched = allCerts.stream()
                        .filter(cert -> cert.contains(safeVehicleNo))
                        .collect(Collectors.toList());
                map.put(v.getVehicleNo(), matched);
            }
        } catch (IOException e) {
            // Return empty map on error
            for (Vehicle v : vehicles) {
                map.put(v.getVehicleNo(), Collections.emptyList());
            }
        }
        return map;
    }
    /**
     * Rename certificate files when a vehicle number is updated.
     * Finds certificate files containing the old vehicle number and renames them
     * to use the new vehicle number, preserving the certificate-to-vehicle matching.
     */
    public void renameCertificatesForVehicle(String category, String id,
                                             String oldVehicleNo, String newVehicleNo) throws IOException {
        String safeOldNo = sanitizeFilename(oldVehicleNo);
        String safeNewNo = sanitizeFilename(newVehicleNo);

        if (safeOldNo.equals(safeNewNo)) {
            return; // No rename needed
        }

        String directoryPath = buildDirectoryPath(category, id);
        Path directory = Paths.get(certificatePath, directoryPath);

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> files = Files.list(directory)) {
            List<Path> matchingFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().contains(safeOldNo))
                    .collect(Collectors.toList());

            for (Path oldPath : matchingFiles) {
                String oldName = oldPath.getFileName().toString();
                String newName = oldName.replace(safeOldNo, safeNewNo);
                Path newPath = oldPath.resolveSibling(newName);
                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
