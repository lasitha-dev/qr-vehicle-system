package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VehicleType Entity - Maps to the 'vehicle_types' table
 * Lookup table for vehicle categories (Car, Motorcycle, Van, etc.)
 */
@Entity
@Table(name = "vehicle_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", length = 50)
    private String typeName;

    @Column(name = "icon", length = 10)
    private String icon; // Emoji icon (🏍, 🚗, etc.)

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    /**
     * Display format: "🚗 Car"
     */
    public String getDisplayName() {
        return (icon != null ? icon + " " : "") + typeName;
    }
}
