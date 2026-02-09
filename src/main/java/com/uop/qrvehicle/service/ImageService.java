package com.uop.qrvehicle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Image Service - Manages profile images and ID card photos
 * Migrated from: view-update-image.php, view-update-imagemod.php, view-update-imagenow.php
 *
 * Directory structure:
 *   images/Student/<id>.jpg
 *   images/Staff/<empno>.jpg
 *   images/Visitor/<id>.jpg
 *   images/Old/<original_name>_<timestamp>.jpg  (archive)
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Value("${app.upload.image-path}")
    private String imagePath;

    /**
     * Upload a profile image, archiving the old one if it exists.
     * Mirrors PHP logic: move old to Old/ folder, save new.
     */
    public String uploadProfileImage(MultipartFile file, String category, String id)
            throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        // Validate image type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Build category subdirectory
        String categoryDir = normalizeCategoryDir(category);
        Path directory = Paths.get(imagePath, categoryDir);
        Files.createDirectories(directory);

        // Determine filename
        String safeId = id.replace("/", "_").replace("\\", "_");
        String extension = getExtension(file.getOriginalFilename(), contentType);
        String filename = safeId + extension;
        Path filePath = directory.resolve(filename);

        // Archive old image if exists
        if (Files.exists(filePath)) {
            archiveImage(filePath, safeId);
        }

        // Save new image
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Profile image uploaded: {}", filePath);

        return "/uploads/images/" + categoryDir + "/" + filename;
    }

    /**
     * Get profile image URL for a person
     */
    public String getProfileImageUrl(String category, String id) {
        String categoryDir = normalizeCategoryDir(category);
        String safeId = id.replace("/", "_").replace("\\", "_");

        // Try common extensions
        for (String ext : new String[]{".jpg", ".jpeg", ".png", ".gif"}) {
            Path filePath = Paths.get(imagePath, categoryDir, safeId + ext);
            if (Files.exists(filePath)) {
                return "/uploads/images/" + categoryDir + "/" + safeId + ext;
            }
        }

        return null;
    }

    /**
     * Check if a profile image exists
     */
    public boolean hasProfileImage(String category, String id) {
        return getProfileImageUrl(category, id) != null;
    }

    /**
     * List archived images for a person
     */
    public List<String> listArchivedImages(String id) throws IOException {
        List<String> archived = new ArrayList<>();
        String safeId = id.replace("/", "_").replace("\\", "_");
        Path oldDir = Paths.get(imagePath, "Old");

        if (Files.exists(oldDir) && Files.isDirectory(oldDir)) {
            try (Stream<Path> files = Files.list(oldDir)) {
                files.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().startsWith(safeId + "_"))
                     .forEach(p -> archived.add(p.getFileName().toString()));
            }
        }
        return archived;
    }

    /**
     * Delete a profile image
     */
    public boolean deleteProfileImage(String category, String id) throws IOException {
        String categoryDir = normalizeCategoryDir(category);
        String safeId = id.replace("/", "_").replace("\\", "_");

        for (String ext : new String[]{".jpg", ".jpeg", ".png", ".gif"}) {
            Path filePath = Paths.get(imagePath, categoryDir, safeId + ext);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Profile image deleted: {}", filePath);
                return true;
            }
        }
        return false;
    }

    /**
     * Archive an existing image by moving it to Old/ directory with timestamp
     */
    private void archiveImage(Path existingPath, String safeId) throws IOException {
        Path oldDir = Paths.get(imagePath, "Old");
        Files.createDirectories(oldDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = getFileExtension(existingPath.getFileName().toString());
        String archiveName = safeId + "_" + timestamp + ext;

        Path archivePath = oldDir.resolve(archiveName);
        Files.move(existingPath, archivePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Archived old image: {} → {}", existingPath, archivePath);
    }

    /**
     * Map category name to directory
     */
    private String normalizeCategoryDir(String category) {
        if (category == null) return "Other";
        return switch (category.toLowerCase()) {
            case "student" -> "Student";
            case "permanent", "permanent staff" -> "Staff";
            case "temporary", "temporary staff", "casual", "contract", "institute" -> "Staff";
            case "visitor", "visit" -> "Visitor";
            default -> "Staff";
        };
    }

    private String getExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf(".");
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
