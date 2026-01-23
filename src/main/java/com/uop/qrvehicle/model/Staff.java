package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Staff Entity - Maps to the 'slipspaymentsdetailall' table
 * Stores permanent staff information
 * Note: This table has composite primary key (SalDt, EmpNo)
 */
@Entity
@Table(name = "slipspaymentsdetailall")
@IdClass(StaffId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

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

    @Column(name = "EmpCat1Nm")
    private String category;

    @Column(name = "NIC")
    private String nic;

    @Column(name = "Sex")
    private String sex;

    @Column(name = "BuNm")
    private String department;

    @Column(name = "BuCd")
    private String departmentCode;

    @Column(name = "EmpTypCd")
    private String employeeType; // Academic, Non Academic, Acade Support

    @Column(name = "DtBirth")
    private String dateOfBirth;

    @Column(name = "BrnNm")
    private String branchName;

    // Get display name
    public String getDisplayName() {
        return empNo + " - " + empName + " (" + category + ")";
    }

    // Check if academic staff
    public boolean isAcademic() {
        return "Academic".equalsIgnoreCase(employeeType);
    }
}
