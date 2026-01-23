package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Visitor Entity - Maps to the 'visitor' table
 * Stores visitor information
 * Primary key: ID (varchar)
 */
@Entity
@Table(name = "visitor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visitor {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "Name")
    private String name;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "DateFrom")
    private LocalDateTime dateFrom;

    @Column(name = "DateTo")
    private LocalDateTime dateTo;

    // Check if visitor pass is currently valid
    public boolean isValidToday() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(dateFrom) && !now.isAfter(dateTo);
    }
}
