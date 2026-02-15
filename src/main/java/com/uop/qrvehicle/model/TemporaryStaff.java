package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TemporaryStaff Entity - Maps to the 'temporarystaff' table
 * Stores temporary, casual, contract, and institute staff
 * Note: Uses same structure as slipspaymentsdetailall with composite key (SalDt, EmpNo)
 */
@Entity
@Table(name = "temporarystaff")
@IdClass(TemporaryStaffId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryStaff {

    @Id
    @Column(name = "SalDt")
    private String salaryDate;

    @Id
    @Column(name = "EmpNo")
    private String empNo;

    @Column(name = "EmpNm")
    private String empName;

    @Column(name = "DesgNm")
    private String designation;

    @Id
    @Column(name = "EmpCat1Nm")
    private String category; // Temporary, Casual, Contract, Institute

    @Column(name = "NIC")
    private String nic;

    @Column(name = "Sex")
    private String sex;

    @Column(name = "BuNm")
    private String department;

    @Column(name = "BuCd")
    private String departmentCode;

    @Column(name = "EmpTypCd")
    private String employeeType;

    @Column(name = "DtBirth")
    private String dateOfBirth;

    // Get display name
    public String getDisplayName() {
        return empNo + " - " + empName + " (" + category + ")";
    }

    // Check category type
    public boolean isTemporary() {
        return category != null && category.toLowerCase().contains("temporary");
    }

    public boolean isCasual() {
        return category != null && category.toLowerCase().contains("casual");
    }

    public boolean isContract() {
        return category != null && category.toLowerCase().contains("contract");
    }

    public boolean isInstitute() {
        return category != null && category.toLowerCase().contains("institute");
    }
}
