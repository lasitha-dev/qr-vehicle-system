package com.uop.qrvehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Staff Detail DTO
 * Comprehensive staff profile data from slipspaymentsdetailall table
 * Mirrors PHP view_staff.php and search_person.php staff sections
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffDetailDTO {

    // Core identification
    private String empNo;
    private String empName;
    private String nic;
    private String sex;
    private String dateOfBirth;

    // Employment info
    private String designation;
    private String category;         // EmpCat1Nm (Academic, Non Academic, etc.)
    private String employeeType;     // EmpTypCd (Academic, Non Academic, Acade Support)
    private String department;       // BuNm
    private String departmentCode;   // BuCd
    private String branchName;       // BrnNm
    private String latestSalaryDate;

    // Additional fields (if available from extended query)
    private String address;
    private String mobile;
    private String email;

    // Image
    private String imageUrl;

    // Computed
    private String expiryDate;

    /**
     * Get display category type for the profile
     */
    public String getDisplayCategory() {
        if (category == null) return "Staff";
        return switch (category.toLowerCase()) {
            case "academic" -> "Academic Staff";
            case "non academic" -> "Non-Academic Staff";
            case "acade support" -> "Academic Support Staff";
            default -> category + " Staff";
        };
    }

    /**
     * Get retirement age based on employee type
     */
    public int getRetirementAge() {
        return isAcademic() ? 65 : 60;
    }

    public boolean isAcademic() {
        return "Academic".equalsIgnoreCase(employeeType)
                || "Academic".equalsIgnoreCase(category);
    }

    /**
     * Gender display
     */
    public String getGenderDisplay() {
        if (sex == null) return "—";
        return switch (sex.toUpperCase()) {
            case "M" -> "Male";
            case "F" -> "Female";
            default -> sex;
        };
    }
}
