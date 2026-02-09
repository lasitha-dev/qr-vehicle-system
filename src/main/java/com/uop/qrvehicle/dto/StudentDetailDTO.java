package com.uop.qrvehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Student Detail DTO
 * Mirrors the PHP view.php which joins stud, studbasic, faculty, course, district, studother tables.
 * Contains all the fields shown in the student profile view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailDTO {

    // === Basic Registration Info (from stud table) ===
    private String regNo;
    private String appYear;
    private String faculty;
    private String course;
    private String status;       // REGISTERED, SUSPENDED, DEFERRED, etc.
    private String registeredOn;
    private String selectType;
    private String nic;

    // === Personal Info (from studbasic table) ===
    private String title;
    private String initials;
    private String lastName;
    private String fullName;
    private String gender;
    private String dateOfBirth;
    private String religion;
    private String ethnicity;

    // === Faculty/Course Names (from faculty, course tables) ===
    private String facultyName;
    private String courseName;

    // === Address Info (from studother table) ===
    private String address1;
    private String address2;
    private String address3;
    private String district;
    private String divisionalSecretariat;
    private String policeStation;

    // === Contact Info (from studother table) ===
    private String phone;
    private String mobile;
    private String email;

    // === A/L Results (from studbasic table) ===
    private String alStream;
    private String alDistrict;
    private String zScore;
    private String alYear;

    // === Parent/Guardian Info (from studother table) ===
    private String fatherName;
    private String motherName;
    private String guardianName;
    private String guardianAddress;
    private String guardianPhone;

    // === Emergency Contact ===
    private String emergencyContactName;
    private String emergencyContactPhone;

    // === Semester Info (from studclass table) ===
    private String currentSemester;
    private String semesterName;
    private String semesterRegDate;

    // === Image URL ===
    private String imageUrl;

    // === Academic History ===
    private List<String> promotions = new ArrayList<>();
    private List<String> transfers = new ArrayList<>();
    private List<String> deferrals = new ArrayList<>();
    private List<String> cancellations = new ArrayList<>();

    // === Display helpers ===

    /**
     * Get name with initials (e.g., "MR SILVA A.B.C.")
     */
    public String getNameWithInitials() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty() && !"-".equals(title) && !"REV".equals(title)) {
            sb.append(title.toUpperCase()).append(" ");
        }
        if (lastName != null) sb.append(lastName.toUpperCase());
        if (initials != null) sb.append(" ").append(initials.toUpperCase());
        return sb.toString().trim();
    }

    /**
     * Get formatted address
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (address1 != null && !address1.isEmpty()) sb.append(address1);
        if (address2 != null && !address2.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address2);
        }
        if (address3 != null && !address3.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address3);
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Get status CSS class for color coding
     */
    public String getStatusClass() {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case "SUSPENDED" -> "status-suspended";
            case "DEFERRED" -> "status-deferred";
            case "REGISTERED" -> "status-registered";
            case "CANCELLED" -> "status-cancelled";
            default -> "";
        };
    }
}
