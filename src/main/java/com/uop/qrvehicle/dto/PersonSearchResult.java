package com.uop.qrvehicle.dto;

import com.uop.qrvehicle.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for person search results
 * Unifies data from students, staff, and visitors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonSearchResult {

    private String id;
    private String type; // Student, Permanent Staff, Temporary Staff, Visitor
    private String name;
    private String designation;
    private String category;
    private String department;
    private String nic;
    private String gender;
    private String employeeType;
    private String imageUrl;
    
    // Student specific
    private String faculty;
    private String course;
    private String semester;
    private String registrationDate;
    
    // Visitor specific
    private String reason;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    
    // Vehicles
    private List<Vehicle> vehicles = new ArrayList<>();
    
    // Contact info
    private String mobile;
    private String email;
    private String address;
}
