package com.uop.qrvehicle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Visitor Entity - Maps to the 'visitor' table
 * Stores visitor information
 */
@Entity
@Table(name = "visitor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "Name")
    private String name;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "DateFrom")
    private LocalDate dateFrom;

    @Column(name = "DateTo")
    private LocalDate dateTo;

    // Check if visitor pass is currently valid
    public boolean isValidToday() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(dateFrom) && !today.isAfter(dateTo);
    }
}
