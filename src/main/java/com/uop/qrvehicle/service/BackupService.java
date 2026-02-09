package com.uop.qrvehicle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Database Backup Service
 * Runs mysqldump to create timestamped .sql backups of the vehicle_qr_db database.
 * Migrated from: backup.php
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.backup.path:./backups/}")
    private String backupPath;

    @Value("${app.backup.mysqldump-path:mysqldump}")
    private String mysqldumpPath;

    /**
     * Create a database backup using mysqldump.
     * Returns the path to the generated backup file.
     */
    public String createBackup() throws IOException, InterruptedException {
        // Parse JDBC URL to extract host, port, database
        String host = "localhost";
        int port = 3306;
        String database = "vehicle_qr_db";

        // Parse jdbc:mysql://host:port/database?params
        String url = datasourceUrl;
        if (url.contains("://")) {
            String afterProtocol = url.substring(url.indexOf("://") + 3);
            // host:port/database?params
            if (afterProtocol.contains("/")) {
                String hostPort = afterProtocol.substring(0, afterProtocol.indexOf("/"));
                String rest = afterProtocol.substring(afterProtocol.indexOf("/") + 1);

                if (hostPort.contains(":")) {
                    host = hostPort.substring(0, hostPort.indexOf(":"));
                    port = Integer.parseInt(hostPort.substring(hostPort.indexOf(":") + 1));
                } else {
                    host = hostPort;
                }

                database = rest.contains("?") ? rest.substring(0, rest.indexOf("?")) : rest;
            }
        }

        // Ensure backup directory exists
        Path backupDir = Paths.get(backupPath);
        Files.createDirectories(backupDir);

        // Generate timestamped filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = database + "_" + timestamp + ".sql";
        Path backupFile = backupDir.resolve(filename);

        // Build mysqldump command
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(true);

        if (dbPassword != null && !dbPassword.isEmpty()) {
            pb.command(mysqldumpPath,
                    "-u", dbUsername,
                    "-p" + dbPassword,
                    "-h", host,
                    "-P", String.valueOf(port),
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    database);
        } else {
            pb.command(mysqldumpPath,
                    "-u", dbUsername,
                    "-h", host,
                    "-P", String.valueOf(port),
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    database);
        }

        pb.redirectOutput(backupFile.toFile());

        log.info("Starting database backup: {} -> {}", database, backupFile);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            long fileSize = Files.size(backupFile);
            log.info("Backup completed successfully: {} ({} bytes)", backupFile, fileSize);
            return backupFile.toAbsolutePath().toString();
        } else {
            // Read error output
            String errorOutput = "";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                errorOutput = reader.lines().reduce("", (a, b) -> a + "\n" + b);
            }
            log.error("Backup failed with exit code {}: {}", exitCode, errorOutput);
            // Clean up failed backup file
            Files.deleteIfExists(backupFile);
            throw new IOException("mysqldump failed with exit code " + exitCode + ": " + errorOutput);
        }
    }

    /**
     * List all existing backup files
     */
    public java.util.List<BackupInfo> listBackups() throws IOException {
        java.util.List<BackupInfo> backups = new java.util.ArrayList<>();
        Path backupDir = Paths.get(backupPath);

        if (!Files.exists(backupDir)) {
            return backups;
        }

        try (var stream = Files.list(backupDir)) {
            stream.filter(p -> p.toString().endsWith(".sql"))
                  .sorted((a, b) -> {
                      try {
                          return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                      } catch (IOException e) {
                          return 0;
                      }
                  })
                  .forEach(path -> {
                      try {
                          backups.add(new BackupInfo(
                                  path.getFileName().toString(),
                                  Files.size(path),
                                  Files.getLastModifiedTime(path).toInstant().toString()
                          ));
                      } catch (IOException e) {
                          log.debug("Could not read backup file info: {}", path);
                      }
                  });
        }

        return backups;
    }

    /**
     * Get a backup file for download
     */
    public Path getBackupFile(String filename) {
        // Sanitize filename to prevent directory traversal
        String safeName = Paths.get(filename).getFileName().toString();
        return Paths.get(backupPath, safeName);
    }

    /**
     * Delete a backup file
     */
    public boolean deleteBackup(String filename) throws IOException {
        Path file = getBackupFile(filename);
        if (Files.exists(file)) {
            Files.delete(file);
            log.info("Backup deleted: {}", filename);
            return true;
        }
        return false;
    }

    /**
     * Backup file info record
     */
    public record BackupInfo(String filename, long sizeBytes, String createdAt) {
        public String getFormattedSize() {
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
}
