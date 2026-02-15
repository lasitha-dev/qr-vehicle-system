package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vehicle Entity - Maps to the 'vehidb' table
 * Stores vehicle registration information
 * Composite Primary Key: (EmpID, Vehino)
 */
@Entity
@Table(name = "vehidb")
@IdClass(VehicleId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @Column(name = "EmpID")
    private String empId; // Employee/Student ID

    @Id
    @Column(name = "Vehino")
    private String vehicleNo;

    @Column(name = "VehiOwner")
    private String owner;

    @Column(name = "Type")
    private String type; // Student, Permanent, Temporary, Casual, Contract, Institute, Visitor

    // New: Vehicle Type relationship (FK to vehicle_types table)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    // Mobile number field
    @Column(name = "Mobile", length = 12)
    private String mobile;

    // Email field
    @Column(name = "Email", columnDefinition = "text")
    private String email;

    @Column(name = "ApprovalStatus", columnDefinition = "enum('Pending','Approved','Rejected')")
    private String approvalStatus; // Pending, Approved, Rejected

    @Column(name = "createDate")
    private LocalDateTime createDate;

    @Column(name = "CreatedBy")
    private String createdBy;

    @Column(name = "ApprovalBy")
    private String approvalBy;

    @Column(name = "ApprovalDate")
    private LocalDateTime approvalDate;

    // Notification tracking fields
    @Column(name = "email_sent")
    private Boolean emailSent;

    @Column(name = "change_email_sent")
    private Boolean changeEmailSent;

    @Column(name = "cert_viewed_at")
    private LocalDateTime certViewedAt;

    @Column(name = "last_notified_status", length = 20)
    private String lastNotifiedStatus;

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

    /**
     * Get vehicle type display name (e.g., "🚗 Car")
     */
    public String getVehicleTypeDisplay() {
        return vehicleType != null ? vehicleType.getDisplayName() : "Unknown";
    }
}

