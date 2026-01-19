package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vehicle Entity - Maps to the 'vehidb' table
 * Stores vehicle registration information
 */
@Entity
@Table(name = "vehidb")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EmpID")
    private String empId; // Employee/Student ID

    @Column(name = "Vehino")
    private String vehicleNo;

    @Column(name = "VehiOwner")
    private String owner;

    @Column(name = "Type")
    private String type; // Student, Permanent, Temporary, Casual, Contract, Institute, Visitor

    @Column(name = "ApprovalStatus")
    private String approvalStatus; // Pending, Approved, Rejected

    @Column(name = "createDate")
    private LocalDateTime createDate;

    @Column(name = "CreatedBy")
    private String createdBy;

    // Pre-persist hook to set creation date
    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
        if (approvalStatus == null) {
            approvalStatus = "Pending";
        }
    }

    // Check if vehicle is approved
    public boolean isApproved() {
        return "Approved".equalsIgnoreCase(approvalStatus);
    }

    // Check if vehicle is pending
    public boolean isPending() {
        return "Pending".equalsIgnoreCase(approvalStatus);
    }
}
